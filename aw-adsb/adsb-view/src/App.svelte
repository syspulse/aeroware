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
	//var markerLocations = [];
	var aircraftsList = [];
	var aircraftsMap = {};
	//var aircraftsListUpdated = [];
	
	function decodeData(data) {
		let attr = data.split(",");
		// ts,icaoId,icaoCallsign,lon,lat,alt,hSpeed,vRate,heading
		return [
			Number(attr[0]),
			attr[1],
			attr[2],

			parseFloat(attr[3]),
			parseFloat(attr[4]),
			parseFloat(attr[5]),

			parseFloat(attr[6]),
			parseFloat(attr[7]),
			parseFloat(attr[8]),
		];
	}

	function updateWithData(raw) {
		const data = decodeDataList(raw);
      return update(data);
	}

	function update(data) {
    data.forEach( t => {
      console.log("====> ",t)
      let telemetry = decodeData(t);
			let icaoId = telemetry[1];
			var aircraft = {};
			if(icaoId in aircraftsMap) 
				aircraft = aircraftsMap[icaoId] 
			else {
				aircraft = {
					'icao': icaoId,
					'call':telemetry[2],
					'tel': []
				};
			};
			      

			if(aircraft['tel'] === undefined || aircraft['tel'].length > 5) {
				//aircraft['tel'] = [];
        aircraft['tel'].shift(aircraft['tel'].length - 5);
			}
				aircraft['tel'].push( {
					'lat':telemetry[3],
					'lon':telemetry[4],
					'alt':telemetry[5],

					'ts':telemetry[0],
					'call':telemetry[2],
					
					'hs':telemetry[6],
					'vr': telemetry[7],
					'hd': telemetry[8],
				});
			

			// update
			aircraftsMap[icaoId] = aircraft;
		});

		var aircraftsListUpdated = [];
		for(let key in aircraftsMap)
   		if(aircraftsMap[key] !== undefined)
			   aircraftsListUpdated.push(aircraftsMap[key]);

		//console.log(aircraftsListUpdated);
		return aircraftsListUpdated;
	}

	function decodeDataList(raw) {
		return raw.split("\n").filter( s => s.trim().length!=0);
	}

	onMount(async () => {
        const response = await fetch('http://localhost:5000/data.csv');
        const dataRsp = await response.text();
		
		const data = decodeDataList(dataRsp);
        aircraftsList = update(data);
    })

	const initialView = 
	[50.2490977,30.145533,12.79];
    // [50.3688672,30.1594487,15];
    //[50.4215603,30.1517414,15];
    // [50.420905, 30.170973];
    //[50.694122314453125,30.47310494087838]//[50.4584,30.3381];//[39.8283, -98.5795];
	
	function createMap(container) {
	  let m = L.map(container, {preferCanvas: true }).setView(initialView, 13);
    L.tileLayer(
	    'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png',
	    {
	      attribution: `&copy;<a href="https://www.openstreetmap.org/copyright" target="_blank">OpenStreetMap</a>,
	        &copy;<a href="https://carto.com/attributions" target="_blank">CARTO</a>`,
	      subdomains: 'aeroware',
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
	
	function markerIcon(id,alt,vel) {
		let html = `
		<div class="map-marker">
			<div>${markerIcons.library}</div>
			<div class="marker-id">${id}</div>
			<div class="marker-alt">${alt}</div>
			<div class="marker-vel">${vel}</div>
		</div>`;
		return L.divIcon({
			html,
			className: 'map-marker'
		});
	}
	

	// function createMarker(loc) {
	// 	let altitude = loc[2];
	// 	let aircraft = "";//loc[3];
		
	// 	let icon = markerIcon(altitude);
	// 	let marker = L.marker(loc, {icon});
	// 	bindPopup(marker, (m) => {
	// 		let c = new MarkerPopup({
	// 			target: m,
	// 			props: {
	// 				aircraft,
	// 				altitude
	// 			}
	// 		});
			
	// 		c.$on('change', ({detail}) => {
	// 			altitude = detail;
	// 			marker.setIcon(markerIcon(altitude));
	// 		});
			
	// 		return c;
	// 	});
		
	// 	return marker;
	// }

	function createAircraftMarker(icao,loc) {
		let aircraft = aircraftsMap[icao];
		let callSign = aircraft['call'];
		
		let id = (callSign === undefined || callSign == "") ? icao  : callSign;
		let alt = aircraft['tel'][0]['alt'];
		let hs = aircraft['tel'][0]['hs'];
		
		if(isNaN(hs)) hs = 0; else hs = hs.toFixed(2);

		let icon = markerIcon(id, alt, hs);
		let marker = L.marker(loc, {icon});

		bindPopup(marker, (m) => {
			let c = new MarkerPopup({
				target: m,
				props: {
					icaoId: icao,
					icaoSign: callSign,
					altitude: loc[2]
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
	
	function createLines(markerLocations) {
		return L.polyline(markerLocations, { color: '#E4E', opacity: 0.5 });
	}

	let markerLayers;
	let lineLayers;

  function mapAction(container) {
    map = createMap(container); 
		toolbar.addTo(map);
		
		syncMarkers(aircraftsList);
		
		return {
		destroy: () => {
					toolbar.remove();
					map.remove();
					map = null;
				}
		};
	}

	function syncMarkers(aircraftsList) {
		markerLayers = L.layerGroup()
 		// for(let location of markerLocations) {
 		// 	let m = createMarker(location);
		// 	markerLayers.addLayer(m);
 		// }
		for(let aircraft of aircraftsList) {
			let icao = aircraft['icao'];
			let telemetry = aircraft['tel'];
      
			
			let markerLocations = telemetry.map(t => {
				return [t['lat'],t['lon'],t['alt']];
			});

      // add marker only to last location
			
      // for(let loc of markerLocations) {
			// 	let m = createAircraftMarker(icao,loc);
			// 	markerLayers.addLayer(m);
			// }
      let lastLoc = markerLocations[markerLocations.length-1];
      
      let m = createAircraftMarker(icao,lastLoc);
      createAircraftMarker(icao,)
			markerLayers.addLayer(m);

			lineLayers = createLines(markerLocations);
		
			markerLayers.addTo(map);
			lineLayers.addTo(map);
		}
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

	$: if(aircraftsList && map) syncMarkers(aircraftsList);

	$: {
		let data = $WS;
		if(data) {
			let updatedAircraftList = updateWithData(data);			

			markerLayers.remove();

			// const last = markerLocations.slice(-5);
			// last.push(decoded);
			// markerLocations = last
			syncMarkers(updatedAircraftList);
		}
	}

	function resizeMap() {
	  if(map) { map.invalidateSize(); }
  }

</script>

<svelte:window on:resize={resizeMap} />

<style>
	.map :global(.marker-id) {
		width:100%;
		text-align:center;
		font-weight:600;
		background-color:#444;
		color:#EEE;
		border-radius:0.5rem;
	}

	.map :global(.marker-alt) {
		width:100%;
		text-align:center;
		color:#444;
	}
	.map :global(.marker-vel) {
		width:100%;
		text-align:center;
		color:#444;
	}
	
	.map :global(.map-marker) {
		/* width:30px;
		transform:translateX(-50%) translateY(-25%); */
		width:60px;
	}
	
</style>

<link rel="stylesheet" href="https://unpkg.com/leaflet@1.6.0/dist/leaflet.css"
   integrity="sha512-xwE/Az9zrjBIphAcBb3F6JVqxf46+CDLwfLMHloNu6KEQCAWi6HcDUbeOfBIptF7tcCzusKFjFw2yuvEpDL9wQ=="
   crossorigin=""/>
<div class="map" style="height:100%;width:100%" use:mapAction />