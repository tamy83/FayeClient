var FayePG = function(){};

FayePG.prototype.PInvoke = function(method, data, callbackOK, callbackError){
	console.log('fayePG Pinvoke');
	if(data == null || data === undefined)
		data = [];
	else if(!Array.isArray(data))
	  data = [data];

	cordova.exec(callbackOK, callbackError, 'FayePG', method, data);
};

FayePG.prototype.init = function(address){
	console.log('fayePG init');
	window.plugins.FayePG.PInvoke("init", address, function(){console.log('init success')}, function(){console.log('init error')});
};

FayePG.prototype.disconnect = function(){
	  console.log('fayePG disc');
		  window.plugins.FayePG.PInvoke("disconnect", null, function(){console.log('disc success')}, function(){console.log('disc error')});
};

FayePG.prototype.subscribe = function(channel){
	console.log('fayePG subscribe');
	window.plugins.FayePG.PInvoke("subscribe", channel, function(){console.log('subscribe success');}, function(){console.log('subscribe error')});
};

FayePG.prototype.publish = function(channel, data){
	var args = [channel, data];
	window.plugins.FayePG.PInvoke("publish", args, function(){console.log('publish success')}, function(){console.log('publish error')});
};

cordova.addConstructor(function() {
  console.log('fayePG constructor');
  if (!window.Cordova) {
	  window.Cordova = cordova;
	};
 if(!window.plugins) window.plugins = {};
 window.plugins.FayePG = new FayePG();																						                       });

