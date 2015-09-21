Handlebars.registerHelper('getTitle', function (string, options) {
  var parts = string.split('.');
  return parts[parts.length -1];
});

Handlebars.registerHelper('getIcon', function (string, options) {
  var icons = {
    'service': 'desktop',
    'person': 'user',
    'organization': 'users',
    'article': 'comment',
    'action': 'gears'
  };
  return new Handlebars.SafeString('<i class="fa fa-' + (icons[string] || 'desktop') + '"></i>');
});

Handlebars.registerHelper('json', function (obj, options) {
  return new Handlebars.SafeString(JSON.stringify(obj, null, 2));
});