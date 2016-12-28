if (typeof String.prototype.endsWith !== 'function') {
  String.prototype.endsWith = function(suffix) {
    return this.indexOf(suffix, this.length - suffix.length) !== -1;
  };
}

if (typeof Array.prototype.csOr !== 'function') {
  Array.prototype.csOr = function() {
    if(this.length > 1) {
      return this.slice(0, -1).join(', ') + ' or ' + this[this.length - 1];
    } else {
      return this[0];
    }
  };
}


// From https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/keys
if (!Object.keys) {
  Object.keys = (function() {
    'use strict';
    var hasOwnProperty = Object.prototype.hasOwnProperty,
        hasDontEnumBug = !({ toString: null }).propertyIsEnumerable('toString'),
        dontEnums = [
          'toString',
          'toLocaleString',
          'valueOf',
          'hasOwnProperty',
          'isPrototypeOf',
          'propertyIsEnumerable',
          'constructor'
        ],
        dontEnumsLength = dontEnums.length;

    return function(obj) {
      if (typeof obj !== 'object' && (typeof obj !== 'function' || obj === null)) {
        throw new TypeError('Object.keys called on non-object');
      }

      var result = [], prop, i;

      for (prop in obj) {
        if (hasOwnProperty.call(obj, prop)) {
          result.push(prop);
        }
      }

      if (hasDontEnumBug) {
        for (i = 0; i < dontEnumsLength; i++) {
          if (hasOwnProperty.call(obj, dontEnums[i])) {
            result.push(dontEnums[i]);
          }
        }
      }
      return result;
    };
  }());
}

Handlebars.registerHelper('localized', function(list, options) {

  language = options.hash.language || navigator.language || navigator.userLanguage || "en";

  var result = '';
  // Empty list
  if (!list) {
    return options.inverse(this);
  }
  // Check for entries in requested language
  for (var i = 0; i < list.length; i++) {
    if (list[i]['@language'] == language) {
      result = result + options.fn(list[i]);
    }
  }
  // Requested language not available, default to en
  if (result.trim() == '') {
    for (var i = 0; i < list.length; i++) {
      if (list[i]['@language'] == 'en') {
        result = result + options.fn(list[i]);
      }
    }
  }
  // Neither requested language nor en available, return all of first available
  if (result.trim() == '') {
    for (var i = 0; i < list.length; i++) {
      if (list[i]['@language'] == list[0]['@language']) {
        result = result + options.fn(list[i]);
      }
    }
  }

  if (result.trim() != '') {
    return result;
  } else {
    return options.inverse(this);
  }

});

Handlebars.registerHelper('getField', function (string, options) {
  var parts = string.split('.');
  var field = parts[parts.length -1];
  if (field == "@id") {
    field = parts[parts.length -2];
  }
  return field;
});

Handlebars.registerHelper('getIcon', function (string, options) {
  var type = string || "";
  var icons = {
    'service': 'desktop',
    'person': 'user',
    'organization': 'users',
    'article': 'comment',
    'action': 'gears',
    'concept': 'tag',
    'conceptscheme': 'sitemap',
    'event': 'calendar'
  };
  return new Handlebars.SafeString(
    '<i class="fa fa-fw fa-' + (icons[type.toLowerCase()] || 'question') + '"></i>'
  );
});

Handlebars.registerHelper('getBundle', function (field, options) {
  var bundles = {
    'availableLanguage': 'languages',
    'addressCountry': 'countries'
  }
  return bundles[field] || 'messages';
});

Handlebars.registerHelper('getResourceUrl', function (url, options) {
  return new Handlebars.SafeString("/resource/" + url);
});

Handlebars.registerHelper('externalLink', function (url, options) {

  var icon = 'fa-external-link-square';

  if (url.indexOf('twitter.com') > -1) {
    icon = 'fa-twitter-square';
  } else if (url.indexOf('facebook.com') > -1) {
    icon = 'fa-facebook-square';
  } else if (url.indexOf('instagram.com') > -1) {
    icon = 'fa-instagram';
  } else if (url.indexOf('linkedin.com') > -1) {
    icon = 'fa-linkedin-square';
  } else if (url.indexOf('youtube.com') > -1) {
    icon = 'fa-youtube-square';
  }

  return new Handlebars.SafeString('<i class="fa fa-fw ' + icon + '"></i><a href="' + url + '">' + url + '</a>');

});

Handlebars.registerHelper('removeFilterLink', function (filter, value, href) {
  var matchFilter = new RegExp("[?&]filter." + filter + "=" + value, "g");
  var matchFrom = new RegExp("from=\\d+", "g");
  return new Handlebars.SafeString(
    href.replace(matchFilter, '').replace(matchFrom, 'from=0')
  );
});

/**
 * Adopted from http://stackoverflow.com/questions/7261318/svg-chart-generation-in-javascript
 */
  Handlebars.registerHelper('pieChart', function(aggregation, colors, options) {

  var buckets = aggregation['buckets'];

  var width = options.hash.width || 400;
  var height = options.hash.height || 400;
  var donut_color = options.hash.donat_color || '#f7faff';

  function openTag(type, closing, attr) {
      var html = ['<' + type];
      for (var prop in attr) {
        // A falsy value is used to remove the attribute.
        // EG: attr[false] to remove, attr['false'] to add
        if (attr[prop]) {
          html.push(prop + '="' + attr[prop] + '"');
        }
      }
      return html.join(' ') + (!closing ? ' /' : '') + '>';
    }

  function closeTag(type) {
    return '</' + type + '>';
  }

  function createElement(type, closing, attr, contents) {
    return openTag(type, closing, attr) + (closing ? (contents || '') + closeTag(type) : '');
  }

  var total = buckets.reduce(function (accu, that) {
    return that['doc_count'] + accu;
  }, 0);

  var sectorAngleArr = buckets.map(function (v) { return 360 * v['doc_count'] / total; });

  var arcs = [];
  var startAngle = 270;
  var endAngle = 270;

  for (var i=0; i<sectorAngleArr.length; i++) {
    startAngle = endAngle;
    endAngle = startAngle + sectorAngleArr[i];

    var x1,x2,y1,y2 ;

    x1 = parseInt(Math.round((width/2) + ((width/2)*.975)*Math.cos(Math.PI*startAngle/180)));
    y1 = parseInt(Math.round((height/2) + ((height/2)*.975)*Math.sin(Math.PI*startAngle/180)));

    x2 = parseInt(Math.round((width/2) + ((width/2)*.975)*Math.cos(Math.PI*endAngle/180)));
    y2 = parseInt(Math.round((height/2) + ((height/2)*.975)*Math.sin(Math.PI*endAngle/180)));

    var d = "M" + (width/2) + "," + (height/2)+ "  L" + x1 + "," + y1 + "  A" + ((width/2)*.975) + "," + ((height/2)*.975) + " 0 " +
        ((endAngle-startAngle > 180) ? 1 : 0) + ",1 " + x2 + "," + y2 + " z";

    var c = parseInt((i + 200) / sectorAngleArr.length * 360);
    var path = createElement("path", true, {d: d, fill: colors[i], 'class': 'color-' + i});

    var url_params = {};
    for (var p in buckets[i]) {
      url_params[p] = (typeof buckets[i][p] == 'string') ? encodeURIComponent(buckets[i][p]) : buckets[i][p];
    }
    var href = urltemplate.parse(options.hash['href-template']).expand(url_params);
    var arc = createElement("a", true, {
      "xlink:href": href,
      "xlink:title": Handlebars.helpers.i18n(buckets[i]['key'], null) + " (" + buckets[i]['doc_count'] + ")"
    }, path);
    arcs.push(arc);
  }

  var donut = createElement("circle", true, {cx: width/2, cy: height/2, r: height/3, fill: donut_color});

  return new Handlebars.SafeString(createElement("svg" , true, {
    width: width,
    height: height,
    class: "chart",
    viewbox: "0 0 " + width + " " + height,
    xmlns: "http://www.w3.org/2000/svg",
    "xmlns:xlink": "http://www.w3.org/1999/xlink"
  }, arcs.join("") + donut));

});

Handlebars.registerHelper('ifObjectNotEmpty', function(obj, options){
  /*

  Here http://stackoverflow.com/a/679937/1060128 this is suggested to be compatible with Pre-ECMA 5 (IE 8)
  But it doesn't work.

  for(var prop in obj) {
    if(obj.hasOwnProperty(prop)) {
      return options.fn(this);
    }
  }
  */
  if ((!(typeof obj == "object")) || (!Object.keys(obj).length)) {
    return options.inverse(this);
  }

  return options.fn(this);

});



Handlebars.registerHelper('nestedAggregation', function (aggregation) {
  return nestedAggregation(aggregation);
});

function nestedAggregation(aggregation, collapsed, id) {
  collapsed = typeof collapsed !== 'undefined' ? collapsed : false;
  id = typeof id !== 'undefined' ? id : false;

  var list = '<ul class="schema-tree collapse' + (collapsed ? '' : '.in') + '" ' + (id ? 'id="' + id + '"' : '') + '>';
  for (var key in aggregation) {
    if (typeof aggregation[key] == "object") {
      var class_id = key.split("/").slice(-1)[0];
      list +=
        '<li>' +
          '<div class="schema-tree-item">' +
            '<i class="fa fa-fw fa-tag schema-tree-icon"></i>' +
            '<a href="/resource/' + key + '">' +
              Handlebars.helpers.i18n(key, null) + " (" + aggregation[key]["doc_count"] + ")" +
            '</a>' +
            (
              Object.keys(aggregation[key]).length > 1 ?
              '<a href="#' + class_id + '" class="schema-tree-plus collapsed" data-toggle="collapse">' +
                '<i class="fa fa-plus"></i>' +
                '<i class="fa fa-minus"></i>' +
              '</a>' :
              ''
            ) +
          '</div>' +
          nestedAggregation(aggregation[key], true, class_id) +
        '</li>';
    }
  }
  list += "</ul>";
  return Handlebars.SafeString(list);
}

// http://stackoverflow.com/a/16315366
Handlebars.registerHelper('ifCond', function (v1, operator, v2, options) {

  switch (operator) {
    case '==':
      return (v1 == v2) ? options.fn(this) : options.inverse(this);
    case '===':
      return (v1 === v2) ? options.fn(this) : options.inverse(this);
    case '<':
      return (v1 < v2) ? options.fn(this) : options.inverse(this);
    case '<=':
      return (v1 <= v2) ? options.fn(this) : options.inverse(this);
    case '>':
      return (v1 > v2) ? options.fn(this) : options.inverse(this);
    case '>=':
      return (v1 >= v2) ? options.fn(this) : options.inverse(this);
    case '&&':
      return (v1 && v2) ? options.fn(this) : options.inverse(this);
    case '||':
      return (v1 || v2) ? options.fn(this) : options.inverse(this);
    default:
      return options.inverse(this);
  }

});

Handlebars.registerHelper('get', function(list, index) {
  return list[index];
});

Handlebars.registerHelper('encodeURIComponent', function(string) {
  return encodeURIComponent(string);
});

Handlebars.registerHelper('sort', function(context, field, direction, options) {

  var arr = [];
  for (var i in context) {
    arr.push(context[i]);
  }

  arr.sort(function(a, b) {
    if (a[field] > b[field]) {
      return direction == "desc" ? -1 : 1;
    } else if (a[field] < b[field]) {
      return direction == "desc" ? 1 : -1;
    } else {
      return 0;
    }
  });

  var ret = "";

  for(var i=0, j=arr.length; i<j; i++) {
    ret = ret + options.fn(arr[i]);
  }

  return ret;

});

Handlebars.registerHelper('reduceSkos', function(tree, list, options) {
  return filterTree(toNative(tree['hasTopConcept']), toNative(list).map(function(obj){ return obj['@id'] }));
});

function filterTree(tree, list) {
  var res = [];
  for (var i = 0; i < tree.length; i++) {
    if (list.indexOf(tree[i]['@id']) != -1) {
      var leaf = tree[i];
      if (leaf['narrower']) {
        leaf['narrower'] = filterTree(leaf['narrower'], list);
      }
      res.push(leaf);
    }
  }
  return res;
}

function toNative(value) {
  return JSON.parse(JSON.stringify(value));
}

Handlebars.registerHelper('ifIn', function(item, list, options) {
  for (i in list) {
    if (list[i] == item) {
      return options.fn(this);
    }
  }
  return options.inverse(this);
});

Handlebars.registerHelper('exportUrl', function (type, url, extension) {

  if(type == 'list') {
    return url.replace(/\/resource\//, '/resource.' + extension);
  } else if(type == 'detail') {
    return url + '.' + extension;
  }

});

Handlebars.registerHelper('obfuscate', function(string) {

  return string.split('').map(function(char) {
    return '&#' + char.charCodeAt(0) + ';';
  }).join('');

});

//TODO: re-enable markdown
Handlebars.registerHelper('md', function(string) {
  return new Handlebars.SafeString(string);
});

Handlebars.registerHelper('json', function (obj, options) {
  return new Handlebars.SafeString(JSON.stringify(obj, null, 2));
});


Handlebars.registerHelper('stringFormat', function() {

  var args = Array.prototype.slice.call(arguments);
  var format = args[0];
  var params = args.slice(1, args.length - 1);
  var options = args[args.length - 1];

  var size = format.match(/%./g).length;
  for (var i = 0; i < size; i++) {
    format = format.replace(/%./, params[i]);
  }

  return format;

});
