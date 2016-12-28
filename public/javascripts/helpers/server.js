//TODO: implement server side i18n
Handlebars.registerHelper('i18n', function (key, options) {
  return new Handlebars.SafeString(key);
});
