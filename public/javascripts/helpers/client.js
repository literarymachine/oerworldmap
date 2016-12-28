function scroll_to_element(scroll_container, element) {
  $( scroll_container ).animate({
    scrollTop: $( element ).position().top
  }, 500);
}

Handlebars.registerHelper('i18n', function (key, options) {
  var bundle = options.hash.bundle || 'messages';
  return new Handlebars.SafeString(i18nStrings[bundle][key] || key);
});

Handlebars.registerHelper('jsonscript', function(obj) {

  return new Handlebars.SafeString(
    '<script type="application/ld+json">' +
      JSON.stringify(obj, null, 2) +
    '</script>'
  );

});
