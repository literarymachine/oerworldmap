Hijax.behaviours.map = {

  container : null,
  world : null,
  vector : null,
  vectorSource : null,
  projection: null,
  
  standartMapSize: [896, 655],
  standartInitialZoom: 1.85,
  standartMinZoom: 1.85,
  standartMaxZoom: 8,
  
  defaultCenter: [0, 5000000],

  attach : function(context) {
    var map = this;

    $('div[data-view="map"]', context).each(function() {
      
      // Get mercator projection
      map.projection = ol.proj.get('EPSG:3857');

      // Map container
      map.container = $('<div id="map"><div id="popup"></div></div>')[0];
      $(this).prepend(map.container);

      // Vector source
      map.vectorSource = new ol.source.Vector({
        url: '/assets/json/ne_50m_admin_0_countries_topo.json',
        format: new ol.format.TopoJSON(),
        // noWrap: true,
        wrapX: true
      });

      // Vector layer
      map.vector = new ol.layer.Vector({
        source: map.vectorSource
      });
      
      // Get zoom values adapted to map size
      var zoom_values = map.getZoomValues();
      
      // View
      map.view = new ol.View({
        center: map.defaultCenter,
        projection: map.projection,
        zoom: zoom_values.initialZoom,
        minZoom: zoom_values.minZoom,
        maxZoom: zoom_values.maxZoom
      });
      
      // Map object
      map.world = new ol.Map({
        layers: [map.vector],
        target: map.container,
        view: map.view
      });

      // User position
      if (
        navigator.geolocation &&
        ! $(this).attr('data-focus')
      ) {
        navigator.geolocation.getCurrentPosition(function(position) {
          var lon = position.coords.longitude;
          var center = ol.proj.transform([lon, 0], 'EPSG:4326', map.projection.getCode());
          center[1] = map.defaultCenter[1];
          map.world.getView().setCenter(center);
        });
      }

      map.world.on('pointermove', function(evt) {
        if (evt.dragging) {
          return;
        }
        var pixel = map.world.getEventPixel(evt.originalEvent);
        var coord = map.world.getEventCoordinate(evt.originalEvent);
        var hit = map.world.hasFeatureAtPixel(pixel);
        map.world.getTarget().style.cursor = hit ? 'pointer' : '';
        map.displayFeatureInfo(pixel, coord, context);
      });

      map.world.on('click', function(evt) {        
        var feature = map.world.forEachFeatureAtPixel(evt.pixel, function(feature, layer) {
          return feature;
        });
        if (feature) {
          var properties = feature.getProperties();
          if (properties.url) {
            window.location = properties.url;
          } else {
            window.location = "/country/" + feature.getId().toLowerCase();
          }
        }
      });
      
      // zoom to bounding box, if focus is set
      map.setBoundingBox(this);
      
      // init popovers
      map.popupElement = document.getElementById('popup');      
      map.popup = new ol.Overlay({
        element: map.popupElement,
        positioning: 'bottom-center',
        stopEvent: false,
        wrapX: true
      });
      map.world.addOverlay(map.popup);
      map.templates = {};
      $.get('/assets/mustache/popover.mustache', function(data){
        map.templates.popover = data;
      });
      
      // switch style
      $(this).addClass("map-view");

    });

  },
  
  getZoomValues : function() {
    
    if(
      $('#map').width() / $('#map').height()
      >
      this.standartMapSize[0] / this.standartMapSize[1]
    ) {
      // current aspect ratio is more landscape then standart, so take height as constraint
      var size_vactor = $('#map').height() / this.standartMapSize[1];
    } else {
      // it's more portrait, so take width
      var size_vactor = $('#map').width() / this.standartMapSize[0];
    }
    
    size_vactor = Math.pow(size_vactor, 0.5);
    
    return {
      initialZoom: this.standartInitialZoom * size_vactor,
      minZoom: this.standartMinZoom * size_vactor,
      maxZoom: this.standartMaxZoom * size_vactor
    };
    
  },

  displayedFeatureInfo : null,
  displayFeatureInfo : function(pixel, coord, context) {
    var map = this;

    var feature = map.world.forEachFeatureAtPixel(pixel, function(feature, layer) {
      return feature;
    });
    
    var info = $('[about="#users-by-country"]', context);
    info.find('tr').hide();
    info.find('thead>tr').hide();
    if (feature) {
      
      // get which world repitition we are on
      
      var extent = map.projection.getExtent();
      var extent_width = extent[0] * (-1) + extent[2];
      
      if(
        extent[0] <= coord[0] &&
        coord[0] <= extent[2]
      ) {
        // coord in central world
        var world_n = 0;
      } else if(
        coord[0] < extent[2]
      ) {
        // coord in a left world
        var world_n = Math.floor(
          (coord[0] - extent[0]) / extent_width
        );
      } else if(
        extent[2] < coord[0]
      ) {
        // coord in a right world
        var world_n = Math.ceil(
          (coord[0] - extent[2]) / extent_width
        );
      }
    
      if (!(map.displayedFeatureInfo && map.displayedFeatureInfo.getId() == feature.getId())) {
        $(map.popupElement).popover('destroy');
        map.displayedFeatureInfo = feature;
      }
      var properties = feature.getProperties();
      if (properties.type) {
        
        // console.log("pixel", pixel);
        // console.log("coord", coord);
        
        // Feature is an icon to which the resource and references are attached
        // console.log(properties.resource);
        // console.log(properties.refBy);
        
        var popup_coord = feature.getGeometry().getCoordinates();
        popup_coord[0] =
          popup_coord[0] +
          world_n * extent_width;
       
        map.popup.setPosition(popup_coord);
        
        properties.refBy.first = properties.refBy[ Object.keys(properties.refBy)[0] ];
        
        $(map.popupElement).popover({
          'placement': 'top',
          'html': true,
          'container': '#map',
          'title': '<i class="fa fa-users"></i> Organisation',
          'content': Mustache.to_html(map.templates.popover, properties), //properties.resource.legalName['@value'], //feature.get('name')
          'template': '<div class="popover color-scheme-text" role="tooltip"><div class="arrow"></div><h3 class="popover-title"></h3><div class="popover-content"></div></div>'
        });
        $(map.popupElement).popover('show');
        
      } else if (properties.country) {
        // Feature is a country
        console.log(properties.country);
        info.find('thead>tr').show();
        info.find('tr[about="#' + feature.getId().toLowerCase() + '"]').show();
      }
    } else {
      $(map.popupElement).popover('destroy');
    }

  },

  setHeatmapData : function(aggregations) {
console.log(aggregations);
    var map = this;

    var heat_data = {};

    var dataAttached = false;
    map.vectorSource.on('change', function(e) {
      if (map.vectorSource.getState() == 'ready' && !dataAttached) {
        dataAttached = true;
        for(var j = 0; j < aggregations["by_country"]["buckets"].length; j++) {
          var aggregation = aggregations["by_country"]["buckets"][j];
          var feature = map.vectorSource.getFeatureById(aggregation.key.toUpperCase());
          if (feature) {
            var properties = feature.getProperties();
            properties.country = aggregation;
            feature.setProperties(properties);
          }
        }
      }
    });

    for(var j = 0; j < aggregations["by_country"]["buckets"].length; j++) {
      var aggregation = aggregations["by_country"]["buckets"][j];
      heat_data[ aggregation.key.toUpperCase() ] = aggregation["doc_count"];
    }

    var heats = $.map(heat_data, function(value, index) {
      return [value];
    });

    var color = d3.scale.log()
      .range(["#a1cd3f", "#eaf0e2"])
      .interpolate(d3.interpolateHcl)
      .domain([d3.quantile(heats, .01), d3.quantile(heats, .99)]);

    map.vector.setStyle(function(feature) {
      return [new ol.style.Style({
        fill: new ol.style.Fill({
          color: heat_data[ feature.getId() ] ? color( heat_data[ feature.getId() ] ) : "#ffffff"
        }),
        stroke: new ol.style.Stroke({
          color: '#0c75bf',
          width: .5
        })
      })];
    });

  },

  addPlacemarks : function(placemarks) {

    var map = this;

    var vectorSource = new ol.source.Vector({
      features: placemarks,
      wrapX: true
    });

    /*var clusterSource = new ol.source.Cluster({
      distance: 40,
      source: vectorSource,
      noWrap: true,
      wrapX: false
    });

    var styleCache = {};
    var clusterLayer = new ol.layer.Vector({
      source: clusterSource,
      style: function(feature, resolution) {
        var size = feature.get('features').length;
        var style = styleCache[size];
        if (!style) {
          style = [new ol.style.Style({
            image: new ol.style.Circle({
              radius: 10,
              stroke: new ol.style.Stroke({
                color: '#fff'
              }),
              fill: new ol.style.Fill({
                color: '#3399CC'
              })
            }),
            text: new ol.style.Text({
              text: size.toString(),
              fill: new ol.style.Fill({
                color: '#fff'
              })
            })
          })];
          styleCache[size] = style;
        }
        return style;
      }
    });
    map.world.addLayer(clusterLayer);*/

    var vectorLayer = new ol.layer.Vector({
      source: vectorSource
    });

    map.world.addLayer(vectorLayer);

  },

  setBoundingBox : function(element) {

    var map = this;

    var focusId = element.getAttribute("data-focus");

    var focussed = false;

    if (focusId) {
      map.vectorSource.on('change', function(e) {
        if (map.vectorSource.getState() == 'ready' && !focussed) {
          focussed = true;
          map.world.getLayers().forEach(function(layer) {
            var feature = layer.getSource().getFeatureById(focusId);
            var tfn = ol.proj.getTransform('EPSG:4326', map.projection.getCode());
            if (feature) {
              if (feature.getId() == "RU") {
                var extent = ol.extent.applyTransform(ol.extent.boundingExtent([[32, 73], [175, 42]]), tfn);
                console.log(extent);
              } else if (feature.getId() == "US") {
                var extent = ol.extent.applyTransform(ol.extent.boundingExtent([[-133, 52], [-65, 25]]), tfn);
              } else if (feature.getId() == "FR") {
                var extent = ol.extent.applyTransform(ol.extent.boundingExtent([[-5, 51], [-10, 42]]), tfn);
              } else {
                var extent = feature.getGeometry().getExtent();
              }
              if (extent[0] == extent[2]) {
                extent[0] -= 1000000;
                extent[2] += 1000000;
              }
              if (extent[1] == extent[3]) {
                extent[1] -= 1000000;
                extent[3] += 1000000;
              }
              map.world.getView().fitExtent(extent, map.world.getSize());
            }
          });
        }
      });
    }

  },

  markers : {},
  getMarkers : function(resource, labelCallback, origin) {

    origin = origin || resource;
    var that = this;

    if (that.markers[resource['@id']]) {
      for (var i = 0; i < that.markers[resource['@id']].length; i++) {
        var properties = that.markers[resource['@id']][i].getProperties();
        if (!properties.refBy[origin['@id']] && origin['@id'] != resource['@id']) {
          properties.refBy[origin['@id']] = origin;
        }
        that.markers[resource['@id']][i].setProperties(properties);
      }
      return that.markers[resource['@id']];
    }

    var locations = [];
    var markers = [];

    if (resource.location && resource.location instanceof Array) {
      locations = locations.concat(resource.location);
    } else if (resource.location) {
      locations.push(resource.location);
    }

    for (var l in locations) {
      if (geo = locations[l].geo) {
        var point = new ol.geom.Point(ol.proj.transform([geo['lon'], geo['lat']], 'EPSG:4326', that.projection.getCode()));

        var color;
        switch (resource['@type']) {
          case 'Article':
            color = 'red';
            break;
          case 'Organization':
            color = 'blue';
            break;
          default:
            color = 'black';
        }

        var iconStyle = new ol.style.Style({
          text: new ol.style.Text({
            text: '\uf041',
            font: 'normal 1.5em FontAwesome',
            textBaseline: 'Bottom',
            fill: new ol.style.Fill({color: color})
          })
        });

        var featureProperties = {
          resource: resource,
          refBy: {},
          geometry: point,
          url: "/resource/" + resource['@id'],
          type: resource['@type'],
        };
        featureProperties.refBy[origin['@id']] = origin;

        var feature = new ol.Feature(featureProperties);
        feature.setId(resource['@id']);
        feature.setStyle(iconStyle);
        markers.push(feature);
      }
    }


    if (!markers.length) {
      for (var key in resource) {
        if ('referencedBy' == key) {
          continue;
        }
        var value = resource[key];
        if (value instanceof Array) {
          for (var i = 0; i < value.length; i++) {
            if (typeof value[i] == 'object') {
              markers = markers.concat(
                that.getMarkers(value[i], labelCallback, origin)
              );
            }
          }
        } else if (typeof value == 'object') {
          markers = markers.concat(
            that.getMarkers(value, labelCallback, origin)
          );
        }
      }
    }

    that.markers[resource['@id']] = markers;
    return markers;

  }

};


// Mollweide Projection

/*
      proj4.defs('ESRI:53009', '+proj=moll +lon_0=0 +x_0=0 +y_0=0 +a=6371000 ' +
          '+b=6371000 +units=m +no_defs');
*/

      // Configure the Sphere Mollweide projection object with an extent,
      // and a world extent. These are required for the Graticule.
/*
      var sphereMollweideProjection = new ol.proj.Projection({
        code: 'ESRI:53009',
        extent: [-9009954.605703328, -9009954.605703328,
          9009954.605703328, 9009954.605703328],
        worldExtent: [-179, -90, 179, 90]
      });
*/

/*
      map.projection.setWorldExtent(
        [-180, -10, 180, 10]
      );
*/
      
/*
      console.log("extent", map.projection.getExtent());
      console.log("worldExtent", map.projection.getWorldExtent());
      console.log("global", map.projection.isGlobal());
*/
      
/*
      map.projection.setExtent(
        [-20037508.342789244, -1037508.342789244, 20037508.342789244, 1037508.342789244]
      );
*/


/*
      var extent = [-20037508.342789244, -1037508.342789244, 20037508.342789244, 1037508.342789244];
      var constrainPan = function() {
          var visible = map.view.calculateExtent(map.world.getSize());
          var centre = map.view.getCenter();
          var delta;
          var adjust = false;
          if ((delta = extent[0] - visible[0]) > 0) {
              adjust = true;
              centre[0] += delta;
          } else if ((delta = extent[2] - visible[2]) < 0) {
              adjust = true;
              centre[0] += delta;
          }
          if ((delta = extent[1] - visible[1]) > 0) {
              adjust = true;
              centre[1] += delta;
          } else if ((delta = extent[3] - visible[3]) < 0) {
              adjust = true;
              centre[1] += delta;
          }
          if (adjust) {
              map.view.setCenter(centre);
          }
      };
      map.view.on('change:resolution', constrainPan);
      map.view.on('change:center', constrainPan);
*/

      
      // Zoom to extent ... extent seem to be buggy, so this is done by initial zoom relative to mapsize
      // extent format: [minX, minY, maxX, maxY]
/*
      var mapSize = map.world.getSize();
      var extent = map.vector.getSource().getExtent();
      extent = [-13037508.342789244, -5000000, 13037508.342789244, 13000000]
      map.world.getView().fitExtent(extent, mapSize);
*/

      // Grid
      /*var graticule = new ol.Graticule({
        map: map.world,
        strokeStyle: new ol.style.Stroke({
          color: 'rgba(255,120,0,0.9)',
          width: 1.5,
          lineDash: [0.5, 4]
        })
      });*/
