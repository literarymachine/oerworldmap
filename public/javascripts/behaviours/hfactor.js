var Hijax = (function ($, Hijax) {

  var column = null;

  function extractBody(html) {
    return $(html);
  }

  function append(data, element, setLocation) {

    var role = element.attr('role') || 'main';
    var html = Hijax.attachBehaviours($('<div></div>').append(extractBody(data))).children();
    var target = element.attr('target') || '_parent';

    switch (target) {
      case '_self':
        element.replaceWith(html);
        break;
      case '_blank':
        column.attr("role", role);
        column.html(html);
        break;
      case '_parent':
      default:
        column.attr("role", role);
        column.html(html);
        break;
    }

    if (true == setLocation && '_parent' == target) {
      history.pushState(null, null, element.attr('href')
          || element.attr('action') + '?' + $.param(element.find('input')));
    }

  }

  // --- hfactor ---
  var my = {

    init : function(context) {
      column = $('<div role="main" class="color-scheme-text list-view column small">')
                              .append($('body>header', context).nextAll());
      $('body', context).append(column);

      // Override default goto
      Hijax.goto = function(a, setLocation) {
        setLocation = setLocation || false;
        $.get(a.attr('href'))
          .done(function(data) {
            append(data, a, setLocation);
          })
          .fail(function(jqXHR) {
            append(jqXHR.responseText, a, setLocation);
          });
      },

      // Adding popstate event listener to handle browser back button
      window.addEventListener('popstate', function(e) {
        var a = $('<a></a>').attr('href', location.pathname + location.search);
        Hijax.goto(a, false);
        return false;
      });

      return new $.Deferred().resolve();

    },

    attach : function(context) {

      $('a', context).each(function() {

        var a = $(this);

        a.bind('click', function() {
          Hijax.goto(a, true);
          return false;
        });

        if (a.hasClass('transclude')) {
          a.trigger('click');
        }

      });

      $('form', context).submit(function() {

        var form = $(this);

        $.ajax({
          type: form.attr('method'),
          url: form.attr('action'),
          data: form.serialize()
        }).done(function(data) {
          append(data, form, true);
        }).fail(function(jqXHR) {
          append(jqXHR.responseText, form, true);
        });

        return false;

      });

    }

  };

  Hijax.behaviours.hfactor = my;
  return Hijax;

})(jQuery, Hijax);
