<script>
	import { onMount } from 'svelte';
	import { writable } from 'svelte/store'
	import { WS } from './ws.js'

	
	import L from 'leaflet';
	import MapToolbar from './MapToolbar.svelte';
	import MarkerPopup from './MarkerPopup.svelte';
	import * as markerIcons from './markers.js';
	let map;
	
	// const markerLocations = [
	// 	[29.8283, -96.5795],
	// 	[37.8283, -90.5795],
	// 	[43.8283, -102.5795],
	// 	[48.40, -122.5795],
	// 	[43.60, -79.5795],
	// 	[36.8283, -100.5795],
	// 	[38.40, -122.5795],
	// ];
	var markerLocations = []
	
	function decodeData(data) {
		let attr = data.split(",");
		return [parseFloat(attr[0]),parseFloat(attr[1]),parseFloat(attr[2])];
	}

	onMount(async () => {
        const response = await fetch('http://localhost:5000/data.csv');
        const data = await response.text();
		
		const markers = data.split("\n");
        markerLocations = markers.filter( s => s.trim().length!=0).map( s => {
			return( decodeData(s))
		})
		
    })

	const initialView = [50.694122314453125,30.47310494087838]//[50.4584,30.3381];//[39.8283, -98.5795];
	
	function createMap(container) {
	  let m = L.map(container, {preferCanvas: true }).setView(initialView, 12);
    L.tileLayer(
	    'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png',
	    {
	      attribution: `&copy;<a href="https://www.openstreetmap.org/copyright" target="_blank">OpenStreetMap</a>,
	        &copy;<a href="https://carto.com/attributions" target="_blank">CARTO</a>`,
	      subdomains: 'abcd',
	      maxZoom: 14,
	    }
	  ).addTo(m);

    return m;
  }
	
	let eye = true;
	let lines = true;
	
	let toolbar = L.control({ position: 'topright' });
	let toolbarComponent;
	toolbar.onAdd = (map) => {
		let div = L.DomUtil.create('div');
		toolbarComponent = new MapToolbar({
			target: div,
			props: {}
		});

		toolbarComponent.$on('click-eye', ({ detail }) => eye = detail);
		toolbarComponent.$on('click-lines', ({ detail }) => lines = detail);
		toolbarComponent.$on('click-reset', () => {
			map.setView(initialView, 5, { animate: true })
		})

		return div;
	}

	toolbar.onRemove = () => {
		if(toolbarComponent) {
			toolbarComponent.$destroy();
			toolbarComponent = null;
		}
	};
	
	// Create a popup with a Svelte component inside it and handle removal when the popup is torn down.
	// `createFn` will be called whenever the popup is being created, and should create and return the component.
	function bindPopup(marker, createFn) {
		let popupComponent;
		marker.bindPopup(() => {
			let container = L.DomUtil.create('div');
			popupComponent = createFn(container);
			return container;
		});

		marker.on('popupclose', () => {
			if(popupComponent) {
				let old = popupComponent;
				popupComponent = null;
				// Wait to destroy until after the fadeout completes.
				setTimeout(() => {
					old.$destroy();
				}, 500);

			}
		});
	}
	
	let markers = new Map();
	
	function markerIcon(count) {
		let html = `<div class="map-marker"><div>${markerIcons.library}</div><div class="marker-text">${count}</div></div>`;
		return L.divIcon({
			html,
			className: 'map-marker'
		});
	}
	

	function createMarker(loc) {
		let altitude = loc[2];
		let icon = markerIcon(altitude);
		let marker = L.marker(loc, {icon});
		bindPopup(marker, (m) => {
			let c = new MarkerPopup({
				target: m,
				props: {
					altitude
				}
			});
			
			c.$on('change', ({detail}) => {
				altitude = detail;
				marker.setIcon(markerIcon(altitude));
			});
			
			return c;
		});
		
		return marker;
	}
	
	function createLines() {
		return L.polyline(markerLocations, { color: '#E4E', opacity: 0.5 });
	}

	let markerLayers;
	let lineLayers;

  function mapAction(container) {
    map = createMap(container); 
		toolbar.addTo(map);
		
		markerLayers = L.layerGroup()
 		for(let location of markerLocations) {
 			let m = createMarker(location);
			markerLayers.addLayer(m);
 		}
		
		lineLayers = createLines();
		
		markerLayers.addTo(map);
		lineLayers.addTo(map);
		
		return {
		destroy: () => {
					toolbar.remove();
					map.remove();
					map = null;
				}
		};
	}

	function syncMarkers(markers) {
		markerLayers = L.layerGroup()
 		for(let location of markers) {
 			let m = createMarker(location);
			markerLayers.addLayer(m);
 		}
		lineLayers = createLines();
		
		markerLayers.addTo(map);
		lineLayers.addTo(map);
	}
	
	// We could do these in the toolbar's click handler but this is an example
	// of modifying the map with reactive syntax.
	$: if(markerLayers && map) {
		if(eye) {
			markerLayers.addTo(map);
		} else {
			markerLayers.remove();
		}
	}
	
	$: if(lineLayers && map) {
		if(lines) {
			lineLayers.addTo(map);
		} else {
			lineLayers.remove();
		}
	}

	$: if(markerLocations && map) syncMarkers(markerLocations);

	$: {
		let data = $WS;
		if(data) {
			let decoded = decodeData(data);
			console.log("Decoded: '"+decoded+"'");

			markerLayers.remove();

			const last = markerLocations.slice(-5);

			last.push(decodeData(data));
			markerLocations = last
			//syncMarkers(markerLocations);
		}
	}

	function resizeMap() {
	  if(map) { map.invalidateSize(); }
  }

</script>

<svelte:window on:resize={resizeMap} />

<style>
	.map :global(.marker-text) {
		width:100%;
		text-align:center;
		font-weight:600;
		background-color:#444;
		color:#EEE;
		border-radius:0.5rem;
	}
	
	.map :global(.map-marker) {
		/* width:30px;
		transform:translateX(-50%) translateY(-25%); */
		width:40px;
	}
</style>

<link rel="stylesheet" href="https://unpkg.com/leaflet@1.6.0/dist/leaflet.css"
   integrity="sha512-xwE/Az9zrjBIphAcBb3F6JVqxf46+CDLwfLMHloNu6KEQCAWi6HcDUbeOfBIptF7tcCzusKFjFw2yuvEpDL9wQ=="
   crossorigin=""/>
<div class="map" style="height:100%;width:100%" use:mapAction />