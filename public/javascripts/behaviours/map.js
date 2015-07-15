Hijax.behaviours.map = {

  container : null,
  column : null,
  world : null,
  projection : null,
  countryVectorSource : null,
  countryVectorLayer : null,
  placemarksVectorSource : null,
  placemarksVectorLayer : null,
  osmTileSource : null,
  osmTileLayer : null,

  
  standartMapSize: [896, 655],
  standartInitialZoom: 1.85,
  standartMinZoom: 1.85,
  standartMaxZoom: 8,
  
  defaultCenter: [0, 5000000],
  
  iconCodes: {
    'organizations': 'users',
    'users': 'user',
    'stories': 'comment'
  },
  
  colors: {
    'blue-darker': '#2d567b',
    'orange': '#fe8a00'
  },

  init: function(context) {

    var map = this;

    map.column = $('<div role="complementary" class="color-scheme-map column large" data-view="map"></div>');
    $('body', context).append(map.column);

    // Get mercator projection
    map.projection = ol.proj.get('EPSG:3857');

    // Styles
    map.setupStyles();

    // Map container
    map.container = $('<div id="map"></div>')[0];
    map.column.prepend(map.container);

    // Country vector source
    map.countryVectorSource = new ol.source.Vector({
      url: '/assets/json/ne_50m_admin_0_countries_topo.json',
      format: new ol.format.TopoJSON(),
      // noWrap: true,
      wrapX: true
    });

    // Country vector layer
    map.countryVectorLayer = new ol.layer.Vector({
      source: map.countryVectorSource
    });

    // Placemarks vector source
    map.placemarksVectorSource = new ol.source.Vector({
      wrapX: true
    });

    // Placemarks vector layer
    map.placemarksVectorLayer = new ol.layer.Vector({
      source: map.placemarksVectorSource
    });

    // OSM tile source
    map.osmTileSource = new ol.source.OSM({});

    // OSM tile layer
    map.osmTileLayer = new ol.layer.Tile({
      source: map.osmTileSource,
      opacity: 0.5
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
      layers: [map.countryVectorLayer, map.osmTileLayer, map.placemarksVectorLayer],
      target: map.container,
      view: map.view
    });

    map.world.on('pointermove', function(evt) {
      if (evt.dragging) { return; }
      var pixel = map.world.getEventPixel(evt.originalEvent);
      map.updateHoverState(pixel);
    });

    map.world.on('click', function(evt) {
      var feature = map.world.forEachFeatureAtPixel(evt.pixel, function(feature, layer) {
        return feature;
      });
      if (feature) {
        var properties = feature.getProperties();
        var url;
        if (properties.url) {
          url = properties.url;
        } else {
          url = "/country/" + feature.getId().toLowerCase();
          map.world.getView().fit(feature.getGeometry().getExtent(), map.world.getSize());
        }
        Hijax.goto($('<a></a>').attr('href', url), true);
      }
    });

    // init popover
    map.popoverElement = $('<div class="popover fade top in" role="tooltip"><div class="arrow"></div><div class="popover-content"></div></div>')[0];
    map.popover = new ol.Overlay({
      element: map.popoverElement,
      positioning: 'bottom-center',
      stopEvent: false,
      wrapX: true
    });
    map.world.addOverlay(map.popover);

    // switch style
    $(this).addClass("map-view");

    // Defer until vector source is loaded
    var deferred = new $.Deferred();
    if (map.countryVectorSource.getFeatureById("BR")) { // Is this a relieable test?
      deferred.resolve();
    } else {
      var listener = map.countryVectorSource.on('change', function(e) {
        if (map.countryVectorSource.getState() == 'ready') {
          ol.Observable.unByKey(listener);
          deferred.resolve();
        }
      });
    }
    return deferred;
  },

  attach : function(context) {

    var map = this;

    // Populate map with pins from single resources
    $('article.resource-story', context)
      .add($('div.resource-organization', context))
      .add($('div.resource-action', context))
      .each(function() {
        var json = JSON.parse( $(this).find('script[type="application/ld+json"]').html() );
        var markers = map.getMarkers(json, map.getResourceLabel);
        map.addPlacemarks( markers );
        map.world.getView().fit(map.placemarksVectorSource.getExtent(), map.world.getSize());
      });

    // Populate map with pins from resource listings
    // FIXME: don't use class names for js actions -> reorganize behaviours
    $('.populate-map', context).each(function() {
      var json = JSON.parse( $(this).find('script[type="application/ld+json"]').html() );
      var markers = [];
      for (i in json) {
        var markers = markers.concat( map.getMarkers(json[i], map.getResourceLabel) );
      }
      map.addPlacemarks( markers );
    });

    // Link list entries to pins
    $('[data-behaviour="linkedListEntries"]', context).each(function() {
      $( this ).on("mouseenter", "li", function() {
        var id = this.getAttribute("about");
        var json = JSON.parse( $(this).closest("ul").children('script[type="application/ld+json"]').html() );
        var resource = json.filter(function(resource) {
          return resource['@id'] == id;
        })[0];
        var markers = Hijax.behaviours.map.getMarkers(resource);
        for (var i = 0; i < markers.length; i++) {
          markers[i].setStyle(Hijax.behaviours.map.styles.placemark.hover());
        }
      });
      $( this ).on("mouseleave", "li", function() {
        var id = this.getAttribute("about");
        var json = JSON.parse( $(this).closest("ul").children('script[type="application/ld+json"]').html() );
        var resource = json.filter(function(resource) {
          return resource['@id'] == id;
        })[0];
        var markers = Hijax.behaviours.map.getMarkers(resource);
        for (var i = 0; i < markers.length; i++) {
          markers[i].setStyle(Hijax.behaviours.map.styles.placemark.base());
        }
      });
    });

    // Add heat map data
    $('[about="#users-by-country"]', context).each(function() {
      var json = JSON.parse( $(this).find('script[type="application/ld+json"]').html() );
      map.setAggregations( json );
    });

  },
  
  setupStyles : function() {
    
    var map = this;
    
    map.styles = {
      placemark : {
        
        base : function() {
          return new ol.style.Style({
            text: new ol.style.Text({
              text: '\uf041',
              font: 'normal 1.5em FontAwesome',
              textBaseline: 'Bottom',
              fill: new ol.style.Fill({
                color: map.colors['blue-darker']
              }),
              stroke: new ol.style.Stroke({
                color: 'white',
                width: 3
              })
            })
          })
        },
        
        hover : function() {
          return new ol.style.Style({
            text: new ol.style.Text({
              text: '\uf041',
              font: 'normal 2em FontAwesome',
              textBaseline: 'Bottom',
              fill: new ol.style.Fill({
                color: map.colors['orange']
              }),
              stroke: new ol.style.Stroke({
                color: 'white',
                width: 3
              })
            })
          })
        }
      }/*
,

      country : {
        
        base : new ol.style.Style({
          fill: new ol.style.Fill({
            color: heat_data[ feature.getId() ] ? color( heat_data[ feature.getId() ] ) : "#ffffff"
          }),
          stroke: new ol.style.Stroke({
            color: '#0c75bf',
            width: .5
          })
        }),
 
        hover : new ol.style.Style({
          fill: new ol.style.Fill({
            color: heat_data[ feature.getId() ] ? color( heat_data[ feature.getId() ] ) : "#ffffff"
          }),
          stroke: new ol.style.Stroke({
            color: '#0c75bf',
            width: .5
          })
        })
               
      }
*/
    };
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
  
  getFeatureType : function(feature) {
    if( typeof feature == 'object' ) {
      var id = feature.getId();
    } else {
      var id = feature;
    }
    
    if( id.indexOf("urn:uuid") === 0 ) {
      return "placemark";
    } else {
      return "country";
    }
  },
  
  hoverState : {
    id : false
  },
  
  updateHoverState : function(pixel) {
    
    var map = this;
    
    // get feature at pixel
    var feature = map.world.forEachFeatureAtPixel(pixel, function(feature, layer) {
      return feature;
    });
    
    // get feature type
    if(feature) {
      var type = map.getFeatureType( feature );
    }
    
    // the statemachine ...
    if(
      ! map.hoverState.id &&
      feature
    ) {
      // ... no popover yet, but now. show it, update position, content and state
      
      $(map.popoverElement).show();
      map.setPopoverContent( feature, type );
      map.setPopoverPosition( feature, type, pixel );
      map.setFeatureStyle( feature, 'hover' );
      map.hoverState.id = feature.getId();
      map.world.getTarget().style.cursor = 'pointer';
      
    } else if(
      map.hoverState.id &&
      feature &&
      feature.getId() == map.hoverState.id
    ) {
      // ... popover was active for the same feature, just update position
      
      map.setPopoverPosition( feature, type, pixel );
      
    } else if(
      map.hoverState.id &&
      feature &&
      feature.getId() != map.hoverState.id
    ) {
      // ... popover was active for another feature, update position, content and state
            
      map.setPopoverContent( feature, type );
      map.setPopoverPosition( feature, type, pixel );
      map.setFeatureStyle( feature, 'hover' );
      map.resetFeatureStyle( map.hoverState.id );
      map.hoverState.id = feature.getId();
      
    } else if(
      map.hoverState.id &&
      ! feature
    ) {
      // ... popover was active, but now no feature is hovered. hide popover and update state
      
      $(map.popoverElement).hide();
      map.resetFeatureStyle( map.hoverState.id );
      map.hoverState.id = false;
      map.world.getTarget().style.cursor = '';
      
    } else {
      
      // ... do nothing probably – or did i miss somehting?
      
    }
  
  },
  
  setFeatureStyle : function( feature, style ) {
    var map = this;
    var feature_type = map.getFeatureType( feature );
    
    // neither performant nor elegant ...
    if( feature_type == 'country' ) {
      var current_style = map.countryVectorLayer.getStyle()(feature);
      feature.setStyle(new ol.style.Style({
        fill: current_style[0].getFill(),
        stroke: new ol.style.Stroke({
          color: '#0c75bf',
          width: 2
        }),
        zIndex: 10
      }));     
      return;
    }
    
    feature.setStyle(
      map.styles[ feature_type ][ style ]()
    );
  },
  
  resetFeatureStyle : function( feature_id ) {
    var map = this;
    
    if( map.getFeatureType(feature_id) == 'placemark' ) {    
      map.setFeatureStyle(
        map.placemarksVectorSource.getFeatureById( feature_id ),
        'base'
      );
    }
    
    if( map.getFeatureType(feature_id) == 'country' ) {
      var feature = map.countryVectorSource.getFeatureById( feature_id );
      feature.setStyle(
        map.countryVectorLayer.getStyle()(feature)
      );
    }
        
  },
  
  setPopoverPosition : function(feature, type, pixel) {
    var map = this;
    
    var coord = map.world.getCoordinateFromPixel(pixel);
    
    if( type == 'country' ) {
      map.popover.setPosition(coord);
      map.popover.setOffset([0, -20]);
    }
    
    if( type == 'placemark' ) {
      // calculate offset first
      // ... see http://gis.stackexchange.com/questions/151305/get-correct-coordinates-of-feature-when-projection-is-wrapped-around-dateline
      // ... old offest code until commit ae99f9886bdef1c3c517cd8ea91c28ad23126551
      var offset = Math.floor((coord[0] / 40075016.68) + 0.5);
      var popup_coord = feature.getGeometry().getCoordinates();
      popup_coord[0] += (offset * 20037508.34 * 2);
      
      map.popover.setPosition(popup_coord);
      map.popover.setOffset([0, -30]);
    }
  },

  setPopoverContent : function(feature, type) {
    var map = this;
    var properties = feature.getProperties();
    
    if( type == "placemark" ) {
      properties.refBy.first = properties.refBy[ Object.keys(properties.refBy)[0] ];
      var content = Mustache.to_html(
        $('#popover' + properties.type + '\\.mustache').html(),
        properties
      );
    }
    
    if( type == "country" ) {
      
      // setup empty countrydata, if undefined
      if( typeof properties.country == 'undefined' ) {
        properties.country = {
          key : feature.getId()
        }
      }
      
      var country = properties.country;
      
      // set name
      country.name = i18n[ country.key.toUpperCase() ];

      var content = Mustache.to_html(
        $('#popoverCountry\\.mustache').html(),
        country
      );
    }
    
    $(map.popoverElement).find('.popover-content').html( content );

  },

  setAggregations : function(aggregations) {
    
    var map = this;
    
    // attach aggregations to country features
    for(var j = 0; j < aggregations["by_country"]["buckets"].length; j++) {
      var aggregation = aggregations["by_country"]["buckets"][j];
      var feature = map.countryVectorSource.getFeatureById(aggregation.key.toUpperCase());
      if (feature) {
        var properties = feature.getProperties();
        properties.country = aggregation;
        feature.setProperties(properties);
      } else {
        throw 'No feature with id "' + aggregation.key.toUpperCase() + '" found';
      }
    }

    map.setHeatmapColors(aggregations);
    
  },
  
  setHeatmapColors : function(aggregations) {
    
    var map = this;
    
    var heat_data = {};
    
    // determine focused country
    
    var focusId = $('[data-view="map"]').attr("data-focus");

    if(
      focusId &&
      map.getFeatureType(focusId) == "country"
    ) {
      var focused_country = focusId;
    } else {
      var focused_country = false;
    }
    
    // build heat data hashmap

    for(var j = 0; j < aggregations["by_country"]["buckets"].length; j++) {
      var aggregation = aggregations["by_country"]["buckets"][j];
      heat_data[ aggregation.key.toUpperCase() ] = aggregation["doc_count"];
    }
    
    // setup d3 color callback
    
    var heats_arr = _.values(heat_data);
    var get_color = d3.scale.log()
      .range(["#a1cd3f", "#eaf0e2"])
      .interpolate(d3.interpolateHcl)
      .domain([d3.quantile(heats_arr, .01), d3.quantile(heats_arr, .99)]);
      
    // set style callback
    
    map.countryVectorLayer.setStyle(function(feature) {

      if(
        ! focused_country
      ) {
        var stroke_width = 0.5;
        var stroke_color = '#0c75bf';
        var zIndex = 1;
        
        var color = heat_data[ feature.getId() ] ? get_color( heat_data[ feature.getId() ] ) : "#fff";        
      } else if(
        focused_country != feature.getId()
      ) {
        var stroke_width = 0.5;
        var stroke_color = '#0c75bf';
        var zIndex = 1;
        
        var color_rgb = heat_data[ feature.getId() ] ? get_color( heat_data[ feature.getId() ] ) : "#fff";
        var color_d3 = d3.rgb(color_rgb);
        var color = "rgba(" + color_d3.r + "," + color_d3.g + "," + color_d3.b + ",0.9)";
      } else {
        var stroke_width = 2;
        var stroke_color = '#0c75bf';
        var zIndex = 2;
        
        var color = heat_data[ feature.getId() ] ? get_color( heat_data[ feature.getId() ] ) : "#fff";
      }
      
      return [new ol.style.Style({
        fill: new ol.style.Fill({
          color: color
        }),
        stroke: new ol.style.Stroke({
          color: stroke_color,
          width: stroke_width
        }),
        zIndex: zIndex
      })];
      
    });

  },

  addPlacemarks : function(placemarks) {

    var map = this;
    map.placemarksVectorSource.clear();
    map.placemarksVectorSource.addFeatures(placemarks);

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
        feature.setStyle(that.styles['placemark']['base']());
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
      var extent = map.countryVectorLayer.getSource().getExtent();
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
