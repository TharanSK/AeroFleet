document.addEventListener('DOMContentLoaded', () => {
    lucide.createIcons();

    // Canvas & Context
    const canvas = document.getElementById('gridCanvas');
    const ctx = canvas.getContext('2d');

    // Simulation Dimensions
    const GRID_WIDTH = 800;
    const GRID_HEIGHT = 600;

    // Simulation & UI State
    let isRunning = true;
    let stompClient = null;
    let depots = [];
    let noFlyZones = [];
    let drones = [];
    let allOrders = [];

    // Interpolation state for 60FPS smooth flight
    let lastDronePositions = {};
    let targetDronePositions = {};
    let lastSnapshotTime = Date.now();

    // Resize canvas properly
    function resizeCanvas() {
        const rect = canvas.getBoundingClientRect();
        canvas.width = rect.width;
        canvas.height = rect.height;
    }
    window.addEventListener('resize', resizeCanvas);
    resizeCanvas();

    // Coordinate conversion: from [0, 800] x [0, 600] to Screen Canvas pixels
    function toScreenX(x) {
        return (x / GRID_WIDTH) * canvas.width;
    }
    function toScreenY(y) {
        return (y / GRID_HEIGHT) * canvas.height;
    }
    function toGridX(screenX) {
        return Math.round((screenX / canvas.width) * GRID_WIDTH);
    }
    function toGridY(screenY) {
        return Math.round((screenY / canvas.height) * GRID_HEIGHT);
    }

    // Load Depots & NoFlyZones from REST APIs
    async function loadStaticEntities() {
        try {
            const [depotsRes, nfzRes] = await Promise.all([
                fetch('/api/v1/depots'),
                fetch('/api/v1/geofence')
            ]);
            depots = await depotsRes.json();
            noFlyZones = await nfzRes.json();
            renderZonesTab();
        } catch (e) {
            console.error('Error loading static map entities', e);
        }
    }

    // Cardinal Direction Converter
    function getCardinalDirection(deg) {
        const directions = ["N", "NE", "E", "SE", "S", "SW", "W", "NW", "N"];
        const idx = Math.round((((deg % 360) + 360) % 360) / 45.0);
        return directions[Math.min(8, Math.max(0, idx))];
    }

    // Connect WebSocket via SockJS + STOMP
    function initWebSocket() {
        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);
        stompClient.debug = null;

        stompClient.connect({}, () => {
            addLogEntry('SUCCESS', 'Telemetry Online', 'Connected to AeroFleet STOMP Broker');

            stompClient.subscribe('/topic/telemetry', (msg) => {
                const snapshot = JSON.parse(msg.body);
                handleTelemetrySnapshot(snapshot);
            });

            stompClient.subscribe('/topic/alerts', (msg) => {
                const alert = JSON.parse(msg.body);
                addLogEntry(alert.level, alert.title, alert.message);
            });

            stompClient.subscribe('/topic/orders', () => {
                refreshOrders();
            });
        }, () => {
            addLogEntry('DANGER', 'Disconnected', 'Retrying connection in 4s...');
            setTimeout(initWebSocket, 4000);
        });
    }

    function handleTelemetrySnapshot(snapshot) {
        isRunning = snapshot.simulationRunning;
        document.getElementById('top-sim-status').textContent = isRunning ? 'ACTIVE' : 'PAUSED';
        document.getElementById('top-sim-status').className = isRunning ? 'font-semibold text-emerald-400' : 'font-semibold text-amber-400';
        document.getElementById('top-drone-count').textContent = snapshot.totalDrones;
        document.getElementById('top-inflight-count').textContent = snapshot.activeDeliveries;
        document.getElementById('top-delivered-count').textContent = snapshot.completedOrders;

        const secs = snapshot.simTimeElapsedSeconds || 0;
        const hrs = String(Math.floor(secs / 3600)).padStart(2, '0');
        const mins = String(Math.floor((secs % 3600) / 60)).padStart(2, '0');
        const s = String(secs % 60).padStart(2, '0');
        document.getElementById('top-sim-clock').textContent = `${hrs}:${mins}:${s}`;

        const windSpeed = snapshot.windSpeedMps != null ? snapshot.windSpeedMps : 4.0;
        const windDir = snapshot.windDirectionDeg != null ? snapshot.windDirectionDeg : 45.0;
        const cardinal = getCardinalDirection(windDir);
        const cond = snapshot.weatherCondition || 'CLEAR';

        const compassEl = document.getElementById('env-wind-compass');
        if (compassEl) {
            compassEl.textContent = `${windSpeed.toFixed(1)} m/s @ ${Math.round(windDir)}° (${cardinal})`;
        }
        const condEl = document.getElementById('env-condition');
        if (condEl) {
            condEl.textContent = cond;
            condEl.className = cond === 'STORM' ? 'font-semibold text-rose-400' : (cond === 'WINDY' ? 'font-semibold text-amber-400' : 'font-semibold text-emerald-400');
        }

        const dragBase = cond === 'STORM' ? 2.5 : (cond === 'RAIN' ? 1.5 : (cond === 'WINDY' ? 1.3 : 1.0));
        const totalDrag = dragBase + (windSpeed / 20.0);
        const dragBadge = document.getElementById('env-drag-badge');
        if (dragBadge) {
            dragBadge.textContent = `${totalDrag.toFixed(2)}x Drag`;
            dragBadge.className = totalDrag > 2.0 ? 'font-mono text-[10px] bg-rose-950 text-rose-300 px-2 py-0.5 rounded border border-rose-800' : 'font-mono text-[10px] bg-slate-800 text-slate-300 px-2 py-0.5 rounded border border-slate-700';
        }

        // Save positions for smooth lerp
        const now = Date.now();
        snapshot.drones.forEach(d => {
            if (targetDronePositions[d.droneId]) {
                lastDronePositions[d.droneId] = {
                    x: targetDronePositions[d.droneId].x,
                    y: targetDronePositions[d.droneId].y
                };
            } else {
                lastDronePositions[d.droneId] = { x: d.x, y: d.y };
            }
            targetDronePositions[d.droneId] = { x: d.x, y: d.y };
        });
        lastSnapshotTime = now;

        drones = snapshot.drones;
        renderDroneCards(drones);
    }

    // 60FPS Canvas Render Loop
    function render() {
        ctx.clearRect(0, 0, canvas.width, canvas.height);

        // 1. Draw Background Grid
        drawGrid();

        // 2. Draw No-Fly Zones
        drawNoFlyZones();

        // 3. Draw Charging Hubs / Depots
        drawDepots();

        // 4. Draw Flight Paths & Destination Pins
        drawFlightPaths();

        // 5. Draw Drones with spinning rotors
        drawDrones();

        requestAnimationFrame(render);
    }

    function drawGrid() {
        const w = canvas.width;
        const h = canvas.height;

        // Dark radar background
        ctx.fillStyle = '#090d16';
        ctx.fillRect(0, 0, w, h);

        // Grid lines every 50 units
        ctx.lineWidth = 1;
        for (let gx = 0; gx <= GRID_WIDTH; gx += 50) {
            const sx = toScreenX(gx);
            ctx.strokeStyle = (gx % 100 === 0) ? '#1e293b' : '#141c2e';
            ctx.beginPath();
            ctx.moveTo(sx, 0);
            ctx.lineTo(sx, h);
            ctx.stroke();

            // Coordinate labels along top
            if (gx % 100 === 0 && gx > 0 && gx < GRID_WIDTH) {
                ctx.fillStyle = '#475569';
                ctx.font = '10px monospace';
                ctx.fillText(`X:${gx}`, sx + 3, 15);
            }
        }

        for (let gy = 0; gy <= GRID_HEIGHT; gy += 50) {
            const sy = toScreenY(gy);
            ctx.strokeStyle = (gy % 100 === 0) ? '#1e293b' : '#141c2e';
            ctx.beginPath();
            ctx.moveTo(0, sy);
            ctx.lineTo(w, sy);
            ctx.stroke();

            // Coordinate labels along left
            if (gy % 100 === 0 && gy > 0 && gy < GRID_HEIGHT) {
                ctx.fillStyle = '#475569';
                ctx.font = '10px monospace';
                ctx.fillText(`Y:${gy}`, 6, sy - 4);
            }
        }

        // Airspace Sector Labels
        ctx.fillStyle = '#334155';
        ctx.font = 'bold 11px sans-serif';
        ctx.fillText('SECTOR A-1 [WEST]', toScreenX(40), toScreenY(40));
        ctx.fillText('CENTRAL AIRSPACE [GRID 400x300]', toScreenX(320), toScreenY(40));
        ctx.fillText('SECTOR B-2 [EAST]', toScreenX(680), toScreenY(40));
    }

    function drawNoFlyZones() {
        noFlyZones.forEach(zone => {
            const cx = toScreenX(zone.centerX);
            const cy = toScreenY(zone.centerY);
            const r = (zone.radius / GRID_WIDTH) * canvas.width;

            if (zone.active) {
                // Active restricted zone: Crimson danger ring
                ctx.fillStyle = 'rgba(244, 63, 94, 0.14)';
                ctx.beginPath();
                ctx.arc(cx, cy, r, 0, Math.PI * 2);
                ctx.fill();

                ctx.strokeStyle = '#f43f5e';
                ctx.lineWidth = 1.8;
                ctx.setLineDash([6, 4]);
                ctx.beginPath();
                ctx.arc(cx, cy, r, 0, Math.PI * 2);
                ctx.stroke();
                ctx.setLineDash([]);

                // Label
                ctx.fillStyle = '#fda4af';
                ctx.font = 'bold 10px sans-serif';
                ctx.textAlign = 'center';
                ctx.fillText(`⚠️ ${zone.name}`, cx, cy - 6);
                ctx.font = '9px monospace';
                ctx.fillStyle = '#f43f5e';
                ctx.fillText(`Radius: ${zone.radius}u (ACTIVE)`, cx, cy + 8);
                ctx.textAlign = 'left';
            } else {
                // Inactive / Disabled zone: Muted Slate dashed ring
                ctx.fillStyle = 'rgba(100, 116, 139, 0.05)';
                ctx.beginPath();
                ctx.arc(cx, cy, r, 0, Math.PI * 2);
                ctx.fill();

                ctx.strokeStyle = '#64748b';
                ctx.lineWidth = 1;
                ctx.setLineDash([3, 5]);
                ctx.beginPath();
                ctx.arc(cx, cy, r, 0, Math.PI * 2);
                ctx.stroke();
                ctx.setLineDash([]);

                // Label
                ctx.fillStyle = '#94a3b8';
                ctx.font = 'italic 10px sans-serif';
                ctx.textAlign = 'center';
                ctx.fillText(`⛔ [INACTIVE] ${zone.name}`, cx, cy - 5);
                ctx.textAlign = 'left';
            }
        });
    }

    function drawDepots() {
        depots.forEach(depot => {
            const x = toScreenX(depot.x);
            const y = toScreenY(depot.y);
            const size = 32;

            // Hub Base Box
            ctx.fillStyle = '#0284c7';
            ctx.strokeStyle = '#38bdf8';
            ctx.lineWidth = 2;

            ctx.beginPath();
            ctx.roundRect(x - size/2, y - size/2, size, size, 6);
            ctx.fill();
            ctx.stroke();

            // Hub Icon
            ctx.fillStyle = '#ffffff';
            ctx.font = 'bold 12px sans-serif';
            ctx.textAlign = 'center';
            ctx.fillText('HUB', x, y + 4);

            // Hub Name & Pads Info
            ctx.fillStyle = '#7dd3fc';
            ctx.font = 'bold 10px sans-serif';
            ctx.fillText(depot.name, x, y - size/2 - 5);

            ctx.fillStyle = '#a7f3d0';
            ctx.font = '9px monospace';
            const freePads = depot.chargingPadsTotal - depot.chargingPadsOccupied;
            ctx.fillText(`${freePads}/${depot.chargingPadsTotal} Free`, x, y + size/2 + 12);
            ctx.textAlign = 'left';
        });
    }

    function drawFlightPaths() {
        drones.forEach(d => {
            const isFlying = d.status !== 'IDLE' && d.status !== 'CHARGING' && d.status !== 'MAINTENANCE';
            if (!isFlying) return;

            const sx = toScreenX(d.x);
            const sy = toScreenY(d.y);

            // If remaining waypoints are available, draw full continuous corridor polyline
            if (d.remainingWaypoints && d.remainingWaypoints.length > 0) {
                ctx.strokeStyle = getStatusColor(d.status);
                ctx.lineWidth = 1.8;
                ctx.setLineDash([5, 4]);
                ctx.beginPath();
                ctx.moveTo(sx, sy);
                d.remainingWaypoints.forEach(wp => {
                    ctx.lineTo(toScreenX(wp.x), toScreenY(wp.y));
                });
                ctx.stroke();
                ctx.setLineDash([]);

                // Draw intermediate detour dots & final destination pin
                d.remainingWaypoints.forEach((wp, idx) => {
                    const wx = toScreenX(wp.x);
                    const wy = toScreenY(wp.y);
                    const isLast = (idx === d.remainingWaypoints.length - 1);

                    if (isLast) {
                        ctx.fillStyle = '#f43f5e';
                        ctx.beginPath();
                        ctx.arc(wx, wy, 5, 0, Math.PI * 2);
                        ctx.fill();
                        ctx.strokeStyle = '#ffffff';
                        ctx.lineWidth = 1;
                        ctx.stroke();

                        ctx.fillStyle = '#fbcfe8';
                        ctx.font = 'bold 9px monospace';
                        ctx.fillText(`Target (${Math.round(wp.x)}, ${Math.round(wp.y)})`, wx + 7, wy - 3);
                    } else {
                        // Intermediate detour waypoint dot
                        ctx.fillStyle = '#38bdf8';
                        ctx.beginPath();
                        ctx.arc(wx, wy, 3, 0, Math.PI * 2);
                        ctx.fill();
                    }
                });
            } else if (d.targetX != null && d.targetY != null) {
                // Fallback to direct leg line
                const tx = toScreenX(d.targetX);
                const ty = toScreenY(d.targetY);

                ctx.strokeStyle = getStatusColor(d.status);
                ctx.lineWidth = 1.5;
                ctx.setLineDash([4, 4]);
                ctx.beginPath();
                ctx.moveTo(sx, sy);
                ctx.lineTo(tx, ty);
                ctx.stroke();
                ctx.setLineDash([]);

                ctx.fillStyle = '#f43f5e';
                ctx.beginPath();
                ctx.arc(tx, ty, 5, 0, Math.PI * 2);
                ctx.fill();
                ctx.strokeStyle = '#ffffff';
                ctx.lineWidth = 1;
                ctx.stroke();

                ctx.fillStyle = '#fbcfe8';
                ctx.font = 'bold 9px monospace';
                ctx.fillText(`Target (${Math.round(d.targetX)}, ${Math.round(d.targetY)})`, tx + 7, ty - 3);
            }
        });
    }

    function drawDrones() {
        const timeFraction = Math.min(1.0, (Date.now() - lastSnapshotTime) / 1000.0);
        const rotorAngle = (Date.now() / 35) % (Math.PI * 2);

        drones.forEach(drone => {
            // Smooth position interpolation
            let curX = drone.x;
            let curY = drone.y;
            if (lastDronePositions[drone.droneId] && targetDronePositions[drone.droneId]) {
                const lx = lastDronePositions[drone.droneId].x;
                const ly = lastDronePositions[drone.droneId].y;
                const tx = targetDronePositions[drone.droneId].x;
                const ty = targetDronePositions[drone.droneId].y;
                curX = lx + (tx - lx) * timeFraction;
                curY = ly + (ty - ly) * timeFraction;
            }

            const x = toScreenX(curX);
            const y = toScreenY(curY);
            const color = getStatusColor(drone.status);
            const isFlying = drone.status !== 'IDLE' && drone.status !== 'CHARGING';

            // Rotor arms (Cross)
            ctx.strokeStyle = '#64748b';
            ctx.lineWidth = 2;
            ctx.beginPath();
            ctx.moveTo(x - 12, y - 12);
            ctx.lineTo(x + 12, y + 12);
            ctx.moveTo(x - 12, y + 12);
            ctx.lineTo(x + 12, y - 12);
            ctx.stroke();

            // 4 Rotors
            const rotorOffsets = [
                [-12, -12], [12, -12], [-12, 12], [12, 12]
            ];
            rotorOffsets.forEach(([ox, oy]) => {
                ctx.strokeStyle = isFlying ? '#38bdf8' : '#94a3b8';
                ctx.lineWidth = 1.2;
                ctx.beginPath();
                ctx.arc(x + ox, y + oy, 4.5, 0, Math.PI * 2);
                ctx.stroke();

                if (isFlying) {
                    ctx.beginPath();
                    ctx.moveTo(x + ox + Math.cos(rotorAngle) * 4.5, y + oy + Math.sin(rotorAngle) * 4.5);
                    ctx.lineTo(x + ox - Math.cos(rotorAngle) * 4.5, y + oy - Math.sin(rotorAngle) * 4.5);
                    ctx.stroke();
                }
            });

            // Center Drone Body
            ctx.fillStyle = '#0f172a';
            ctx.strokeStyle = color;
            ctx.lineWidth = 2.5;
            ctx.beginPath();
            ctx.arc(x, y, 7.5, 0, Math.PI * 2);
            ctx.fill();
            ctx.stroke();

            // Direction arrow if moving
            if (isFlying) {
                const rad = Math.toRadians ? Math.toRadians(drone.headingDegrees) : (drone.headingDegrees * Math.PI / 180);
                const hx = x + Math.cos(rad) * 12;
                const hy = y + Math.sin(rad) * 12;
                ctx.strokeStyle = '#fbbf24';
                ctx.lineWidth = 2;
                ctx.beginPath();
                ctx.moveTo(x, y);
                ctx.lineTo(hx, hy);
                ctx.stroke();
            }

            // Callsign Label
            ctx.fillStyle = '#f8fafc';
            ctx.font = 'bold 10px sans-serif';
            ctx.textAlign = 'center';
            ctx.fillText(drone.callsign, x, y - 16);

            // Mini Battery Bar
            const barW = 24;
            const barH = 3;
            ctx.fillStyle = '#1e293b';
            ctx.fillRect(x - barW/2, y + 15, barW, barH);

            ctx.fillStyle = drone.batteryPercent < 25 ? '#f43f5e' : (drone.batteryPercent < 50 ? '#f59e0b' : '#10b981');
            ctx.fillRect(x - barW/2, y + 15, (drone.batteryPercent / 100.0) * barW, barH);

            ctx.textAlign = 'left';
        });
    }

    function getStatusColor(status) {
        switch (status) {
            case 'IDLE': return '#94a3b8';
            case 'CHARGING': return '#38bdf8';
            case 'ASSIGNED': return '#818cf8';
            case 'EN_ROUTE_DELIVERY': return '#10b981';
            case 'DELIVERING': return '#f59e0b';
            case 'RETURNING': return '#38bdf8';
            case 'LANDING': return '#a78bfa';
            case 'EMERGENCY_RTH': return '#f43f5e';
            default: return '#64748b';
        }
    }

    // Mouse Tracking on Grid
    canvas.addEventListener('mousemove', (e) => {
        const rect = canvas.getBoundingClientRect();
        const gx = toGridX(e.clientX - rect.left);
        const gy = toGridY(e.clientY - rect.top);
        document.getElementById('mouse-coords').textContent = `Grid: (X: ${gx}, Y: ${gy})`;
    });

    // Click on Blank Slate Grid to Dispatch Order, Place Zone, or Remove Zone
    canvas.addEventListener('click', (e) => {
        const rect = canvas.getBoundingClientRect();
        const gx = toGridX(e.clientX - rect.left);
        const gy = toGridY(e.clientY - rect.top);

        // 1. If Add Zone modal is open, fill coordinates
        if (zoneModal && !zoneModal.classList.contains('hidden')) {
            document.getElementById('zone-x').value = gx;
            document.getElementById('zone-y').value = gy;
            return;
        }

        // 2. If user clicked on an existing zone, open Remove Zone dialog
        const clickedZone = noFlyZones.find(z => {
            const dist = Math.sqrt((gx - z.centerX) ** 2 + (gy - z.centerY) ** 2);
            return dist <= z.radius;
        });
        if (clickedZone) {
            openRemoveZoneModal(clickedZone.id);
            return;
        }

        // 3. Otherwise dispatch order
        document.getElementById('cust-x').value = gx;
        document.getElementById('cust-y').value = gy;
        openModal();
    });

    // Sidebar Drone Telemetry Cards
    function renderDroneCards(drones) {
        const cardContainer = document.getElementById('drone-card-container');
        cardContainer.innerHTML = '';

        drones.forEach(drone => {
            const statusColor = getStatusColor(drone.status);
            const card = document.createElement('div');
            card.className = 'bg-slate-950/80 border border-slate-800 rounded-xl p-3 shadow-md hover:border-slate-700 transition space-y-2.5';
            card.innerHTML = `
                <div class="flex items-center justify-between">
                    <div class="flex items-center gap-2">
                        <span class="w-2.5 h-2.5 rounded-full" style="background-color: ${statusColor};"></span>
                        <span class="font-bold text-xs text-white">${drone.callsign}</span>
                    </div>
                    <span class="text-[10px] px-2 py-0.5 rounded font-mono font-semibold" style="background: ${statusColor}22; color: ${statusColor};">
                        ${drone.status}
                    </span>
                </div>

                <!-- Battery Progress -->
                <div>
                    <div class="flex justify-between text-[11px] mb-1">
                        <span class="text-slate-400">Battery</span>
                        <span class="font-mono font-semibold ${drone.batteryPercent < 25 ? 'text-rose-400 animate-pulse' : 'text-slate-200'}">
                            ${drone.batteryPercent.toFixed(1)}%
                        </span>
                    </div>
                    <div class="w-full h-1.5 bg-slate-800 rounded-full overflow-hidden">
                        <div class="h-full rounded-full transition-all duration-500" 
                             style="width: ${drone.batteryPercent}%; background-color: ${drone.batteryPercent < 25 ? '#f43f5e' : (drone.batteryPercent < 50 ? '#f59e0b' : '#10b981')};">
                        </div>
                    </div>
                </div>

                <!-- Coordinates & Cargo Grid -->
                <div class="grid grid-cols-3 gap-1 text-[11px] bg-slate-900/60 p-2 rounded-lg text-slate-300">
                    <div>
                        <div class="text-[9px] text-slate-500 uppercase">Pos (X,Y)</div>
                        <div class="font-mono font-semibold text-cyan-300">${Math.round(drone.x)}, ${Math.round(drone.y)}</div>
                    </div>
                    <div>
                        <div class="text-[9px] text-slate-500 uppercase">Speed</div>
                        <div class="font-mono font-semibold text-emerald-300">${drone.speed.toFixed(0)}u/s</div>
                    </div>
                    <div>
                        <div class="text-[9px] text-slate-500 uppercase">Cargo</div>
                        <div class="font-mono font-semibold text-amber-300">${drone.payloadKg.toFixed(1)}kg</div>
                    </div>
                </div>

                <!-- Quick Action Buttons -->
                <div class="flex items-center justify-end gap-1.5 pt-1 text-[11px]">
                    <button class="btn-drain px-2 py-1 rounded bg-slate-800 hover:bg-rose-950 text-slate-400 hover:text-rose-300 border border-slate-700 transition" data-id="${drone.droneId}">
                        ⚡ Drain 30%
                    </button>
                    <button class="btn-recall px-2 py-1 rounded bg-slate-800 hover:bg-amber-950 text-slate-400 hover:text-amber-300 border border-slate-700 transition" data-id="${drone.droneId}">
                        ↩ Recall
                    </button>
                </div>
            `;
            cardContainer.appendChild(card);
        });

        document.querySelectorAll('.btn-recall').forEach(btn => {
            btn.onclick = () => fetch(`/api/v1/drones/${btn.dataset.id}/recall`, { method: 'POST' });
        });
        document.querySelectorAll('.btn-drain').forEach(btn => {
            btn.onclick = () => fetch(`/api/v1/simulation/incident?droneId=${btn.dataset.id}&type=BATTERY_DRAIN&magnitude=30`, { method: 'POST' });
        });
    }

    // Refresh Orders Queue
    async function refreshOrders() {
        try {
            const res = await fetch('/api/v1/orders');
            allOrders = await res.json();
            const container = document.getElementById('order-list-container');
            document.getElementById('order-queue-count').textContent = `${allOrders.length} Orders`;
            container.innerHTML = '';

            allOrders.slice(-15).reverse().forEach(order => {
                const item = document.createElement('div');
                item.className = 'p-2.5 rounded-lg bg-slate-950/70 border border-slate-800 text-xs space-y-1';
                
                let badgeClass = 'bg-slate-800 text-slate-400';
                if (order.status === 'DELIVERED') badgeClass = 'bg-emerald-950 text-emerald-400 border border-emerald-800';
                if (order.status === 'IN_TRANSIT' || order.status === 'ASSIGNED') badgeClass = 'bg-cyan-950 text-cyan-400 border border-cyan-800';
                if (order.status === 'FAILED') badgeClass = 'bg-rose-950 text-rose-400 border border-rose-800';

                item.innerHTML = `
                    <div class="flex justify-between items-center">
                        <span class="font-bold text-slate-200">${order.orderNumber}</span>
                        <span class="text-[10px] px-1.5 py-0.5 rounded font-mono ${badgeClass}">${order.status}</span>
                    </div>
                    <div class="text-slate-400 text-[11px]">${order.customerName} @ (${Math.round(order.deliveryX)}, ${Math.round(order.deliveryY)})</div>
                    <div class="flex justify-between text-[10px] text-slate-500">
                        <span>Weight: ${order.packageWeightKg}kg</span>
                        <span>Priority: ${order.priority}</span>
                    </div>
                `;
                container.appendChild(item);
            });
        } catch (err) {
            console.error('Failed to load orders', err);
        }
    }

    // Mission Event Logs
    function addLogEntry(level, title, message) {
        const feed = document.getElementById('log-feed');
        const entry = document.createElement('div');

        let borderClass = 'border-l-2 border-slate-600 bg-slate-900/50';
        let badgeColor = 'text-slate-400';
        if (level === 'SUCCESS') {
            borderClass = 'border-l-2 border-emerald-500 bg-emerald-950/20';
            badgeColor = 'text-emerald-400';
        } else if (level === 'WARN') {
            borderClass = 'border-l-2 border-amber-500 bg-amber-950/20';
            badgeColor = 'text-amber-400';
        } else if (level === 'DANGER') {
            borderClass = 'border-l-2 border-rose-500 bg-rose-950/20';
            badgeColor = 'text-rose-400';
        } else if (level === 'INFO') {
            borderClass = 'border-l-2 border-cyan-500 bg-cyan-950/20';
            badgeColor = 'text-cyan-400';
        }

        const time = new Date().toLocaleTimeString();

        entry.className = `p-2 rounded ${borderClass} space-y-0.5 transition-all`;
        entry.innerHTML = `
            <div class="flex justify-between items-center text-[10px]">
                <span class="font-bold ${badgeColor}">${title}</span>
                <span class="text-slate-500">${time}</span>
            </div>
            <div class="text-slate-300 text-[11px]">${message}</div>
        `;

        feed.insertBefore(entry, feed.firstChild);
        if (feed.children.length > 40) {
            feed.removeChild(feed.lastChild);
        }
    }

    // Controls
    document.getElementById('btn-toggle-sim').onclick = async () => {
        isRunning = !isRunning;
        await fetch('/api/v1/simulation/control', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ running: isRunning })
        });
        document.getElementById('text-toggle-sim').textContent = isRunning ? 'Pause' : 'Resume';
    };

    document.querySelectorAll('.speed-btn').forEach(btn => {
        btn.onclick = async () => {
            document.querySelectorAll('.speed-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            const speed = parseFloat(btn.dataset.speed);
            await fetch('/api/v1/simulation/control', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ speedMultiplier: speed })
            });
        };
    });

    document.getElementById('weather-select').onchange = async (e) => {
        await fetch('/api/v1/simulation/control', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ weatherCondition: e.target.value })
        });
    };

    document.getElementById('btn-spawn-orders').onclick = async () => {
        await fetch('/api/v1/orders/simulate-batch?count=5', { method: 'POST' });
        refreshOrders();
    };

    document.getElementById('btn-clear-logs').onclick = () => {
        document.getElementById('log-feed').innerHTML = '';
    };

    // Refresh No-Fly Zones from API
    async function refreshZones() {
        try {
            const res = await fetch('/api/v1/geofence');
            noFlyZones = await res.json();
            renderZonesTab();
            if (removeZoneModal && !removeZoneModal.classList.contains('hidden')) {
                renderRemoveZoneModal();
            }
        } catch (e) {
            console.error('Error refreshing zones', e);
        }
    }

    // Render No-Fly Zones in Sidebar Tab
    function renderZonesTab() {
        const container = document.getElementById('zone-list-container');
        if (!container) return;
        container.innerHTML = '';

        if (!noFlyZones || noFlyZones.length === 0) {
            container.innerHTML = '<div class="text-center py-8 text-slate-500 text-xs">No restricted zones established.</div>';
            return;
        }

        noFlyZones.forEach(z => {
            const card = document.createElement('div');
            card.className = 'p-3 rounded-xl bg-slate-950/80 border ' + 
                (z.active ? 'border-rose-900/60 shadow-rose-950/20' : 'border-slate-800') + 
                ' space-y-2 text-xs shadow-md transition';

            const statusBadge = z.active
                ? '<span class="text-[10px] px-2 py-0.5 rounded font-mono font-bold bg-rose-950 text-rose-300 border border-rose-800">ACTIVE</span>'
                : '<span class="text-[10px] px-2 py-0.5 rounded font-mono font-bold bg-slate-800 text-slate-400 border border-slate-700">DISABLED</span>';

            card.innerHTML = `
                <div class="flex items-center justify-between">
                    <div class="flex items-center gap-2">
                        <span class="w-2.5 h-2.5 rounded-full ${z.active ? 'bg-rose-500 animate-pulse' : 'bg-slate-600'}"></span>
                        <span class="font-bold text-slate-100">${z.name}</span>
                    </div>
                    ${statusBadge}
                </div>
                <div class="grid grid-cols-2 gap-2 text-[11px] bg-slate-900/60 p-2 rounded-lg text-slate-300">
                    <div>
                        <div class="text-[9px] text-slate-500 uppercase">Center (X, Y)</div>
                        <div class="font-mono text-cyan-300 font-semibold">${Math.round(z.centerX)}, ${Math.round(z.centerY)}</div>
                    </div>
                    <div>
                        <div class="text-[9px] text-slate-500 uppercase">Radius</div>
                        <div class="font-mono text-rose-400 font-semibold">${Math.round(z.radius)} units</div>
                    </div>
                </div>
                <div class="flex items-center justify-end gap-2 pt-1 border-t border-slate-900 text-[11px]">
                    <button class="btn-toggle-zone px-2.5 py-1 rounded ${z.active ? 'bg-amber-950/60 text-amber-300 border-amber-800/80 hover:bg-amber-900' : 'bg-emerald-950/60 text-emerald-300 border-emerald-800/80 hover:bg-emerald-900'} border transition" data-id="${z.id}">
                        ${z.active ? 'Deactivate' : 'Activate'}
                    </button>
                    <button class="btn-delete-zone px-2.5 py-1 rounded bg-rose-950/60 hover:bg-rose-900 text-rose-300 hover:text-white border border-rose-800/80 transition flex items-center gap-1" data-id="${z.id}">
                        <i data-lucide="trash-2" class="w-3 h-3"></i> Remove
                    </button>
                </div>
            `;
            container.appendChild(card);
        });

        document.querySelectorAll('.btn-toggle-zone').forEach(btn => {
            btn.onclick = async () => {
                await fetch(`/api/v1/geofence/${btn.dataset.id}/toggle`, { method: 'PUT' });
                refreshZones();
            };
        });

        document.querySelectorAll('.btn-delete-zone').forEach(btn => {
            btn.onclick = async () => {
                await fetch(`/api/v1/geofence/${btn.dataset.id}`, { method: 'DELETE' });
                refreshZones();
            };
        });

        lucide.createIcons();
    }

    // Tab buttons
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.onclick = () => {
            document.querySelectorAll('.tab-btn').forEach(b => {
                b.classList.remove('text-cyan-400', 'border-cyan-500');
                b.classList.add('text-slate-400', 'border-transparent');
            });
            btn.classList.add('text-cyan-400', 'border-cyan-500');
            btn.classList.remove('text-slate-400', 'border-transparent');

            document.querySelectorAll('.tab-content').forEach(c => c.classList.add('hidden'));
            document.getElementById(btn.dataset.tab).classList.remove('hidden');
        };
    });

    // Modal: Dispatch Order
    const modal = document.getElementById('order-modal');
    function openModal() { modal.classList.remove('hidden'); }
    function closeModal() { modal.classList.add('hidden'); }

    document.getElementById('btn-open-modal').onclick = openModal;
    document.getElementById('btn-close-modal').onclick = closeModal;
    document.getElementById('btn-cancel-modal').onclick = closeModal;

    document.getElementById('order-form').onsubmit = async (e) => {
        e.preventDefault();
        const payload = {
            customerName: document.getElementById('cust-name').value,
            deliveryX: parseFloat(document.getElementById('cust-x').value),
            deliveryY: parseFloat(document.getElementById('cust-y').value),
            packageWeightKg: parseFloat(document.getElementById('cust-weight').value),
            priority: document.getElementById('cust-priority').value,
            originDepotId: 1
        };

        const res = await fetch('/api/v1/orders', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (res.ok) {
            closeModal();
            refreshOrders();
        }
    };

    // Modal: Define No-Fly Zone
    const zoneModal = document.getElementById('zone-modal');
    function openZoneModal() { 
        if (zoneModal) {
            zoneModal.classList.remove('hidden'); 
            lucide.createIcons();
        }
    }
    function closeZoneModal() { if (zoneModal) zoneModal.classList.add('hidden'); }

    const btnOpenZoneModal = document.getElementById('btn-open-zone-modal');
    if (btnOpenZoneModal) btnOpenZoneModal.onclick = openZoneModal;
    const btnQuickAddZone = document.getElementById('btn-quick-add-zone');
    if (btnQuickAddZone) btnQuickAddZone.onclick = openZoneModal;
    const btnCloseZoneModal = document.getElementById('btn-close-zone-modal');
    if (btnCloseZoneModal) btnCloseZoneModal.onclick = closeZoneModal;
    const btnCancelZoneModal = document.getElementById('btn-cancel-zone-modal');
    if (btnCancelZoneModal) btnCancelZoneModal.onclick = closeZoneModal;

    document.querySelectorAll('.zone-preset-chip').forEach(chip => {
        chip.onclick = () => {
            document.getElementById('zone-name').value = chip.dataset.name;
            document.getElementById('zone-radius').value = chip.dataset.r;
        };
    });

    const zoneForm = document.getElementById('zone-form');
    if (zoneForm) {
        zoneForm.onsubmit = async (e) => {
            e.preventDefault();
            const payload = {
                name: document.getElementById('zone-name').value,
                centerX: parseFloat(document.getElementById('zone-x').value),
                centerY: parseFloat(document.getElementById('zone-y').value),
                radius: parseFloat(document.getElementById('zone-radius').value),
                active: document.getElementById('zone-active').checked
            };

            const res = await fetch('/api/v1/geofence', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (res.ok) {
                closeZoneModal();
                refreshZones();
            }
        };
    }

    // Modal: Remove No-Fly Zone
    const removeZoneModal = document.getElementById('remove-zone-modal');
    function openRemoveZoneModal(highlightId) {
        if (removeZoneModal) {
            removeZoneModal.classList.remove('hidden');
            renderRemoveZoneModal(highlightId);
            lucide.createIcons();
        }
    }
    function closeRemoveZoneModal() {
        if (removeZoneModal) removeZoneModal.classList.add('hidden');
    }

    const btnOpenRemoveZoneModal = document.getElementById('btn-open-remove-zone-modal');
    if (btnOpenRemoveZoneModal) btnOpenRemoveZoneModal.onclick = () => openRemoveZoneModal();
    const btnCloseRemoveZoneModal = document.getElementById('btn-close-remove-zone-modal');
    if (btnCloseRemoveZoneModal) btnCloseRemoveZoneModal.onclick = closeRemoveZoneModal;
    const btnCancelRemoveZoneModal = document.getElementById('btn-cancel-remove-zone-modal');
    if (btnCancelRemoveZoneModal) btnCancelRemoveZoneModal.onclick = closeRemoveZoneModal;

    function renderRemoveZoneModal(highlightId) {
        const listContainer = document.getElementById('remove-zone-list');
        if (!listContainer) return;
        listContainer.innerHTML = '';

        if (!noFlyZones || noFlyZones.length === 0) {
            listContainer.innerHTML = '<div class="text-center py-6 text-slate-500 text-xs">No active or inactive restricted zones exist.</div>';
            return;
        }

        noFlyZones.forEach(z => {
            const isTarget = highlightId && z.id === highlightId;
            const item = document.createElement('div');
            item.className = 'p-3 rounded-lg bg-slate-950 border ' + 
                (isTarget ? 'border-rose-500 ring-1 ring-rose-500' : 'border-slate-800') + 
                ' flex justify-between items-center transition';

            item.innerHTML = `
                <div class="space-y-0.5">
                    <div class="font-bold text-slate-200 flex items-center gap-1.5">
                        <span class="w-2 h-2 rounded-full ${z.active ? 'bg-rose-500' : 'bg-slate-600'}"></span>
                        <span>${z.name}</span>
                        ${isTarget ? '<span class="text-[9px] px-1.5 py-0.5 rounded bg-rose-950 text-rose-300 border border-rose-800">SELECTED</span>' : ''}
                    </div>
                    <div class="text-slate-400 text-[10px] font-mono">
                        Center: (${Math.round(z.centerX)}, ${Math.round(z.centerY)}) | Radius: ${Math.round(z.radius)}u
                    </div>
                </div>
                <div class="flex items-center gap-1.5">
                    <button class="btn-remove-single-zone px-2.5 py-1 rounded bg-rose-950/80 hover:bg-rose-900 text-rose-300 hover:text-white border border-rose-800 text-xs font-semibold flex items-center gap-1 transition" data-id="${z.id}">
                        <i data-lucide="trash-2" class="w-3.5 h-3.5"></i> Remove
                    </button>
                </div>
            `;
            listContainer.appendChild(item);
        });

        document.querySelectorAll('.btn-remove-single-zone').forEach(btn => {
            btn.onclick = async () => {
                await fetch(`/api/v1/geofence/${btn.dataset.id}`, { method: 'DELETE' });
                await refreshZones();
                renderRemoveZoneModal();
            };
        });

        lucide.createIcons();
    }

    const btnClearAllZones = document.getElementById('btn-clear-all-zones');
    if (btnClearAllZones) {
        btnClearAllZones.onclick = async () => {
            if (confirm('Are you sure you want to decommission and remove all restricted zones?')) {
                await fetch('/api/v1/geofence', { method: 'DELETE' });
                await refreshZones();
                closeRemoveZoneModal();
            }
        };
    }

    // Modal: Atmosphere & Environment Controls
    const envModal = document.getElementById('env-modal');
    function openEnvModal() { 
        if (envModal) {
            envModal.classList.remove('hidden'); 
            lucide.createIcons();
            updateEnvEstimate();
        }
    }
    function closeEnvModal() { if (envModal) envModal.classList.add('hidden'); }

    const btnOpenEnvModal = document.getElementById('btn-open-env-modal');
    if (btnOpenEnvModal) btnOpenEnvModal.onclick = openEnvModal;
    const btnCloseEnvModal = document.getElementById('btn-close-env-modal');
    if (btnCloseEnvModal) btnCloseEnvModal.onclick = closeEnvModal;
    const btnCancelEnvModal = document.getElementById('btn-cancel-env-modal');
    if (btnCancelEnvModal) btnCancelEnvModal.onclick = closeEnvModal;

    let selectedWeather = 'CLEAR';
    const sliderWindSpeed = document.getElementById('slider-wind-speed');
    const sliderWindDir = document.getElementById('slider-wind-dir');
    const envWindVal = document.getElementById('env-wind-val');
    const envDirVal = document.getElementById('env-dir-val');
    const envDragEstimate = document.getElementById('env-drag-estimate');

    function updateEnvEstimate() {
        if (!sliderWindSpeed || !sliderWindDir) return;
        const speed = parseFloat(sliderWindSpeed.value);
        const dir = parseFloat(sliderWindDir.value);
        const card = getCardinalDirection(dir);
        if (envWindVal) envWindVal.textContent = `${speed.toFixed(1)} m/s`;
        if (envDirVal) envDirVal.textContent = `${Math.round(dir)}° (${card})`;

        const dragBase = selectedWeather === 'STORM' ? 2.5 : (selectedWeather === 'RAIN' ? 1.5 : (selectedWeather === 'WINDY' ? 1.3 : 1.0));
        const estDrag = dragBase + (speed / 20.0);
        if (envDragEstimate) envDragEstimate.textContent = `${estDrag.toFixed(2)}x`;
    }

    if (sliderWindSpeed) sliderWindSpeed.oninput = updateEnvEstimate;
    if (sliderWindDir) sliderWindDir.oninput = updateEnvEstimate;

    document.querySelectorAll('.env-preset-btn').forEach(btn => {
        btn.onclick = () => {
            selectedWeather = btn.dataset.weather;
            if (sliderWindSpeed && btn.dataset.wind) sliderWindSpeed.value = btn.dataset.wind;
            if (sliderWindDir && btn.dataset.dir) sliderWindDir.value = btn.dataset.dir;
            updateEnvEstimate();
        };
    });

    const btnApplyEnv = document.getElementById('btn-apply-env');
    if (btnApplyEnv) {
        btnApplyEnv.onclick = async () => {
            const speed = parseFloat(sliderWindSpeed.value);
            const dir = parseFloat(sliderWindDir.value);
            await fetch('/api/v1/simulation/control', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    weatherCondition: selectedWeather,
                    windSpeedMps: speed,
                    windDirectionDeg: dir
                })
            });
            document.getElementById('weather-select').value = selectedWeather;
            closeEnvModal();
        };
    }

    // Initialize
    loadStaticEntities();
    initWebSocket();
    refreshOrders();
    requestAnimationFrame(render);
});
