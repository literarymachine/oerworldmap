// --- other ---
Hijax.behaviours.other = {

  attach: function(context) {

    // placeholder polyfill
    $('input, textarea', context).placeholder();

    // call for actions
    $('a[href="#user-register"]', context).click(function(e){
      e.preventDefault();
      $(this).fadeOut();
      $('#user-register').slideDown();
    });
    
    $('[data-action="close"]', context).click(function(e){
      e.preventDefault();
      $(this).parent().slideUp();
      $('a[href="#user-register"]', context).fadeIn();
    });

    // clickable list entries
    $('[data-behaviour="linkedListEntries"]', context).each(function() {
      $( this ).on("click", "li", function(){
        Hijax.goto( $( this ).find("h1 a"), true );
      });
    });

    
  }

}
