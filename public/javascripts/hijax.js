Hijax = {

  deferreds : {},
  behaviours : {},

  attachBehaviour : function(context, behaviour) {
    if ('function' == typeof(Hijax.behaviours[behaviour].attach)) {
      Hijax.deferreds[behaviour].done(function() {
        Hijax.behaviours[behaviour].attach(context);
      });
    }
  },

  attachBehaviours : function(context) {
    for (behaviour in Hijax.behaviours) {
      Hijax.attachBehaviour(context, behaviour);
    }
    return context;
  },

  initBehaviour : function(context, behaviour) {
    if ('function' == typeof(Hijax.behaviours[behaviour].init)) {
      Hijax.deferreds[behaviour] = Hijax.behaviours[behaviour].init(context);
      Hijax.attachBehaviour(context, behaviour);
    } else {
      Hijax.deferreds[behaviour] = new $.Deferred().resolve(context);
      Hijax.attachBehaviour(context, behaviour);
    }
  },

  initBehaviours : function(context) {
    for (var behaviour in Hijax.behaviours) {
      Hijax.initBehaviour(context, behaviour);
    }
    return context;
  },

  goto : function(url) {
    window.location = url;
  },

}
