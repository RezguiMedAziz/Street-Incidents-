document.addEventListener('DOMContentLoaded', function () {
    // Inputs
    const gouvernoratInput = document.getElementById('gouvernoratId');
    const municipaliteInput  = document.getElementById('municipaliteId');

    // Coordonnées par défaut (Tunis)
    const defaultLat = 36.8065;
    const defaultLng = 10.1815;

    // Initialisation de la carte
    const map = L.map('map').setView([defaultLat, defaultLng], 13);

    // OpenStreetMap
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap contributors'
    }).addTo(map);

    // Marker draggable
    const marker = L.marker([defaultLat, defaultLng], {draggable: true}).addTo(map);

    // Fonction pour mettre à jour les inputs
    function updateLocation(lat, lng) {
        document.getElementById('latitude').value = lat.toFixed(6);
        document.getElementById('longitude').value = lng.toFixed(6);

        // Reverse geocoding avec Nominatim
        fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}`, {
            headers: {
                'User-Agent': 'StreetIncidentsApp - student project'
            }
        })
            .then(res => res.json())
            .then(data => {
                const address = data.address || {};

                gouvernoratInput.value = address.state || '';
                municipaliteInput.value = address.suburb || address.county || '';
            })
            .catch(err => console.error('Erreur geocoding: ', err));
    }

    // Event marker drag
    marker.on('dragend', function (e) {
        const pos = marker.getLatLng();
        updateLocation(pos.lat, pos.lng);
    });

    // Event map click
    map.on('click', function (e) {
        marker.setLatLng(e.latlng);
        updateLocation(e.latlng.lat, e.latlng.lng);
    });

    // Initial update avec position par défaut
    updateLocation(defaultLat, defaultLng);
});
