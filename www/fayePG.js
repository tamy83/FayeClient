var FayePG = function(){};

FayePG.prototype.PInvoke = function(method, data, callbackOK, callbackError){
	if(data == null || data === undefined)
		data = [];
	else if(!Array.isArray(data))
	  data = [data];

	cordova.exec(callbackOK, callbackError, 'FayePG', method, data);
};

FayePG.prototype.init = function(address, authToken){
	var args = [address, authToken];
	window.plugins.FayePG.PInvoke("init", args, function(){console.log('init success')}, function(){console.log('init error')});
};

FayePG.prototype.disconnect = function(){
		  window.plugins.FayePG.PInvoke("disconnect", null, function(){console.log('disc success')}, function(){console.log('disc error')});
};

FayePG.prototype.subscribe = function(channel, commandCallback){
  var args = [channel, commandCallback];
	window.plugins.FayePG.PInvoke("subscribe", args, function(){console.log('subscribe success');}, function(){console.log('subscribe error')});
};

FayePG.prototype.sendMessage = function(channel, data){
	var args = [channel, data];
	window.plugins.FayePG.PInvoke("sendMessage", args, function(){console.log('publish success')}, function(){console.log('publish error')});
};

cordova.addConstructor(function() {
  if (!window.Cordova) {
	  window.Cordova = cordova;
	};
 if(!window.plugins) window.plugins = {};
 window.plugins.FayePG = new FayePG();																						                       });

