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
        } catch (e) {
            console.error('Error loading static map entities', e);
        }
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

        document.getElementById('env-wind').textContent = `${snapshot.windSpeedMps.toFixed(1)} m/s`;
        document.getElementById('env-condition').textContent = snapshot.weatherCondition;

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

            // Fill
            ctx.fillStyle = 'rgba(244, 63, 94, 0.12)';
            ctx.beginPath();
            ctx.arc(cx, cy, r, 0, Math.PI * 2);
            ctx.fill();

            // Dashed outline
            ctx.strokeStyle = 'rgba(244, 63, 94, 0.7)';
            ctx.lineWidth = 1.5;
            ctx.setLineDash([5, 4]);
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
            ctx.fillText(`Radius: ${zone.radius}u`, cx, cy + 8);
            ctx.textAlign = 'left';
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
            if (isFlying && d.targetX != null && d.targetY != null) {
                const sx = toScreenX(d.x);
                const sy = toScreenY(d.y);
                const tx = toScreenX(d.targetX);
                const ty = toScreenY(d.targetY);

                // Flight path line
                ctx.strokeStyle = getStatusColor(d.status);
                ctx.lineWidth = 1.5;
                ctx.setLineDash([4, 4]);
                ctx.beginPath();
                ctx.moveTo(sx, sy);
                ctx.lineTo(tx, ty);
                ctx.stroke();
                ctx.setLineDash([]);

                // Target Destination Pin
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

    // Click on Blank Slate Grid to Dispatch Order
    canvas.addEventListener('click', (e) => {
        const rect = canvas.getBoundingClientRect();
        const gx = toGridX(e.clientX - rect.left);
        const gy = toGridY(e.clientY - rect.top);

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

    document.getElementById('btn-trigger-storm').onclick = async () => {
        document.getElementById('weather-select').value = 'STORM';
        await fetch('/api/v1/simulation/control', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ weatherCondition: 'STORM' })
        });
    };

    document.getElementById('btn-clear-logs').onclick = () => {
        document.getElementById('log-feed').innerHTML = '';
    };

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

    // Modal
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

    // Initialize
    loadStaticEntities();
    initWebSocket();
    refreshOrders();
    requestAnimationFrame(render);
});
