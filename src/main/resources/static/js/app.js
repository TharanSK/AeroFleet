document.addEventListener('DOMContentLoaded', () => {
    lucide.createIcons();

    // Map State
    let map = null;
    let depotMarkers = {};
    let droneMarkers = {};
    let dronePaths = {};
    let nfzCircles = [];

    // Telemetry & UI State
    let isRunning = true;
    let stompClient = null;
    let allOrders = [];

    // Initialize Leaflet Map
    function initMap() {
        map = L.map('map', {
            zoomControl: false,
            attributionControl: false
        }).setView([37.7749, -122.4194], 13);

        L.control.zoom({ position: 'bottomright' }).addTo(map);

        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 18,
            subdomains: ['a', 'b', 'c']
        }).addTo(map);

        // Map Click to capture destination coordinates
        map.on('click', (e) => {
            const lat = e.latlng.lat.toFixed(4);
            const lng = e.latlng.lng.toFixed(4);
            document.getElementById('cust-lat').value = lat;
            document.getElementById('cust-lon').value = lng;
            openModal();
        });

        loadDepots();
        loadNoFlyZones();
    }

    // Load Depots from REST API
    async function loadDepots() {
        try {
            const res = await fetch('/api/v1/depots');
            const depots = await res.json();
            depots.forEach(depot => {
                const depotIcon = L.divIcon({
                    className: 'depot-marker',
                    html: `<div class="p-1.5 flex items-center justify-center font-bold text-[10px]">🏠</div>`,
                    iconSize: [28, 28],
                    iconAnchor: [14, 14]
                });

                const marker = L.marker([depot.latitude, depot.longitude], { icon: depotIcon }).addTo(map);
                marker.bindPopup(`
                    <div class="p-2 space-y-1">
                        <div class="font-bold text-cyan-400 text-xs">${depot.name}</div>
                        <div class="text-[11px] text-slate-400">Pads: ${depot.chargingPadsTotal} Total</div>
                        <div class="text-[11px] text-slate-400">Elevation: ${depot.elevationMeters}m</div>
                    </div>
                `, { className: 'custom-popup' });

                depotMarkers[depot.id] = marker;
            });
        } catch (err) {
            console.error('Failed to load depots', err);
        }
    }

    // Load NoFlyZones from REST API
    async function loadNoFlyZones() {
        try {
            const res = await fetch('/api/v1/geofence');
            const zones = await res.json();
            zones.forEach(zone => {
                const circle = L.circle([zone.centerLatitude, zone.centerLongitude], {
                    color: '#f43f5e',
                    fillColor: '#f43f5e',
                    fillOpacity: 0.15,
                    weight: 2,
                    dashArray: '5, 5',
                    radius: zone.radiusMeters,
                    className: 'pulse-circle'
                }).addTo(map);

                circle.bindPopup(`
                    <div class="p-2 space-y-1">
                        <div class="font-bold text-rose-400 text-xs flex items-center gap-1">
                            ⚠️ RESTRICTED AIRSPACE
                        </div>
                        <div class="text-[11px] font-semibold text-slate-200">${zone.name}</div>
                        <div class="text-[10px] text-slate-400">Radius: ${zone.radiusMeters}m | Ceiling: ${zone.maxAltitudeMeters}m</div>
                    </div>
                `, { className: 'custom-popup' });

                nfzCircles.push(circle);
            });
        } catch (err) {
            console.error('Failed to load NoFlyZones', err);
        }
    }

    // Connect WebSocket via SockJS + STOMP
    function initWebSocket() {
        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);
        stompClient.debug = null; // Disable debug noise

        stompClient.connect({}, () => {
            addLogEntry('SUCCESS', 'Telemetry Link Online', 'Connected to AeroFleet STOMP Message Broker');

            // Subscribe to Telemetry Snapshot
            stompClient.subscribe('/topic/telemetry', (message) => {
                const snapshot = JSON.parse(message.body);
                handleTelemetrySnapshot(snapshot);
            });

            // Subscribe to Airspace Alerts
            stompClient.subscribe('/topic/alerts', (message) => {
                const alert = JSON.parse(message.body);
                addLogEntry(alert.level, alert.title, alert.message);
            });

            // Subscribe to Order Updates
            stompClient.subscribe('/topic/orders', () => {
                refreshOrders();
            });
        }, (err) => {
            addLogEntry('DANGER', 'Telemetry Error', 'Connection failed, retrying in 4s...');
            setTimeout(initWebSocket, 4000);
        });
    }

    // Process Incoming Telemetry
    function handleTelemetrySnapshot(snapshot) {
        // Update Header Stats
        isRunning = snapshot.simulationRunning;
        document.getElementById('top-sim-status').textContent = isRunning ? 'ACTIVE' : 'PAUSED';
        document.getElementById('top-sim-status').className = isRunning ? 'font-semibold text-emerald-400' : 'font-semibold text-amber-400';
        document.getElementById('top-drone-count').textContent = snapshot.totalDrones;
        document.getElementById('top-inflight-count').textContent = snapshot.activeDeliveries;
        document.getElementById('top-delivered-count').textContent = snapshot.completedOrders;

        // Sim Clock Formatter (seconds to HH:MM:SS)
        const secs = snapshot.simTimeElapsedSeconds || 0;
        const hrs = String(Math.floor(secs / 3600)).padStart(2, '0');
        const mins = String(Math.floor((secs % 3600) / 60)).padStart(2, '0');
        const s = String(secs % 60).padStart(2, '0');
        document.getElementById('top-sim-clock').textContent = `${hrs}:${mins}:${s}`;

        // Environmental Info
        document.getElementById('env-wind').textContent = `${snapshot.windSpeedMps.toFixed(1)} m/s @ ${snapshot.windDirectionDeg.toFixed(0)}°`;
        document.getElementById('env-condition').textContent = snapshot.weatherCondition;

        // Render Drones on Map & Sidebar
        renderDrones(snapshot.drones);
    }

    function renderDrones(drones) {
        const cardContainer = document.getElementById('drone-card-container');
        cardContainer.innerHTML = '';

        drones.forEach(drone => {
            const isFlying = drone.status !== 'IDLE' && drone.status !== 'CHARGING' && drone.status !== 'MAINTENANCE';
            const statusColor = getStatusColor(drone.status);

            // Update or Create Map Marker
            const droneHtml = `
                <div class="relative flex items-center justify-center transform drone-marker" style="transform: rotate(${drone.headingDegrees}deg);">
                    <div class="w-7 h-7 rounded-full flex items-center justify-center shadow-lg border-2" 
                         style="background: #0f172a; border-color: ${statusColor};">
                        <span class="text-xs">🚁</span>
                    </div>
                </div>
            `;

            const customIcon = L.divIcon({
                className: 'drone-icon-wrap',
                html: droneHtml,
                iconSize: [28, 28],
                iconAnchor: [14, 14]
            });

            if (droneMarkers[drone.droneId]) {
                droneMarkers[drone.droneId].setLatLng([drone.latitude, drone.longitude]);
                droneMarkers[drone.droneId].setIcon(customIcon);
            } else {
                const marker = L.marker([drone.latitude, drone.longitude], { icon: customIcon }).addTo(map);
                marker.bindPopup(`
                    <div class="p-2 space-y-1 text-xs">
                        <div class="font-bold text-cyan-400">${drone.callsign}</div>
                        <div class="text-[11px] text-slate-300">Model: ${drone.modelType}</div>
                        <div class="text-[11px] text-slate-300">Status: <span class="font-semibold" style="color: ${statusColor};">${drone.status}</span></div>
                        <div class="text-[11px] text-slate-400">Battery: ${drone.batteryPercent.toFixed(1)}%</div>
                        <div class="text-[11px] text-slate-400">Altitude: ${drone.altitudeMeters.toFixed(1)}m | Speed: ${drone.speedMps.toFixed(1)}m/s</div>
                    </div>
                `, { className: 'custom-popup' });
                droneMarkers[drone.droneId] = marker;
            }

            // Draw or update flight path polyline
            if (isFlying && drone.targetLat && drone.targetLon) {
                const pathCoords = [
                    [drone.latitude, drone.longitude],
                    [drone.targetLat, drone.targetLon]
                ];
                if (dronePaths[drone.droneId]) {
                    dronePaths[drone.droneId].setLatLngs(pathCoords);
                } else {
                    dronePaths[drone.droneId] = L.polyline(pathCoords, {
                        color: statusColor,
                        weight: 2,
                        dashArray: '4, 6',
                        opacity: 0.8
                    }).addTo(map);
                }
            } else if (dronePaths[drone.droneId]) {
                map.removeLayer(dronePaths[drone.droneId]);
                delete dronePaths[drone.droneId];
            }

            // Build Sidebar Drone Card
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

                <!-- Metrics Grid -->
                <div class="grid grid-cols-3 gap-1 text-[11px] bg-slate-900/60 p-2 rounded-lg text-slate-300">
                    <div>
                        <div class="text-[9px] text-slate-500 uppercase">Alt</div>
                        <div class="font-mono font-semibold text-cyan-300">${drone.altitudeMeters.toFixed(0)}m</div>
                    </div>
                    <div>
                        <div class="text-[9px] text-slate-500 uppercase">Speed</div>
                        <div class="font-mono font-semibold text-emerald-300">${drone.speedMps.toFixed(1)}m/s</div>
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

        // Attach action events to card buttons
        document.querySelectorAll('.btn-recall').forEach(btn => {
            btn.onclick = () => recallDrone(btn.dataset.id);
        });
        document.querySelectorAll('.btn-drain').forEach(btn => {
            btn.onclick = () => injectDrain(btn.dataset.id);
        });
    }

    function getStatusColor(status) {
        switch (status) {
            case 'IDLE': return '#94a3b8';
            case 'CHARGING': return '#38bdf8';
            case 'ASSIGNED': return '#818cf8';
            case 'TAKING_OFF': return '#34d399';
            case 'EN_ROUTE_DELIVERY': return '#10b981';
            case 'DESCENDING_DELIVERY': return '#facc15';
            case 'DELIVERING': return '#f59e0b';
            case 'ASCENDING_DELIVERY': return '#34d399';
            case 'RETURNING': return '#38bdf8';
            case 'LANDING': return '#a78bfa';
            case 'EMERGENCY_RTH': return '#f43f5e';
            default: return '#64748b';
        }
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
                    <div class="text-slate-400 text-[11px]">${order.customerName}</div>
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

    // Controls and Action Handlers
    async function recallDrone(droneId) {
        await fetch(`/api/v1/drones/${droneId}/recall`, { method: 'POST' });
    }

    async function injectDrain(droneId) {
        await fetch(`/api/v1/simulation/incident?droneId=${droneId}&type=BATTERY_DRAIN&magnitude=30`, { method: 'POST' });
    }

    // UI Buttons
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

    // Tabs
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
            deliveryLatitude: parseFloat(document.getElementById('cust-lat').value),
            deliveryLongitude: parseFloat(document.getElementById('cust-lon').value),
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

    // Initial Startup
    initMap();
    initWebSocket();
    refreshOrders();
});
