var FayeClient = function(){};

FayeClient.PInvoke = function(method, data, callbackOK, callbackError){
    console.log('fayeClient Pinvoke');
    if(data == null || data === undefined)
        data = [];
    else if(!Array.isArray(data))
        data = [data];
    
    if (device.platform == "iOS")
        cordova.exec(callbackOK, callbackError, 'FayeClient', method, data);
};

FayeClient.init = function(address){
    console.log('fayeClient init');
    window.plugins.FayeClient.PInvoke("init", address, function(){console.log('init success')}, function(){console.log('init error')});
};

FayeClient.subscribe = function(channel, callbackOK){
    window.plugins.FayeClient.PInvoke("subscribe", channel, callbackOK, function(){console.log('subscribe error')});
};

FayeClient.publish = function(channel, data){
    var args = [channel, data];    
    window.plugins.FayeClient.PInvoke("publish", args, function(){console.log('publish success')}, function(){console.log('publish error')});
};

cordova.addConstructor(function() {
                       console.log('faye client constructor');
                       if (!window.Cordova) {
                       window.Cordova = cordova;
                       };
                       
                       
                       if(!window.plugins) window.plugins = {};
                       window.plugins.FayeClient = new FayeClient();
                       });