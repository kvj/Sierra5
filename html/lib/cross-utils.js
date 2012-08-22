var PLATFORM_WEB = 0;
var PLATFORM_TIT = 1;
var PLATFORM_GAP = 2;
var PLATFORM_AIR = 3;

var detectPlatform = function() {//Detects on which platform application is running
    if (typeof(Titanium) != 'undefined') {//Ti
        return PLATFORM_TIT;
    };
    if (typeof(window) != 'undefined' && window.navigator && navigator.userAgent.indexOf('Android') != -1
            || navigator.userAgent.indexOf('iPhone') != -1
            || navigator.userAgent.indexOf('iPad') != -1
            || window.location.toString().indexOf('?mobile') != -1) {
        CURRENT_PLATFORM_MOBILE = true;
        CURRENT_EVENT_CLICK = 'touchstart';
        CURRENT_EVENT_DOWN = 'touchstart';
        CURRENT_EVENT_MOVE = 'touchmove';
        CURRENT_EVENT_UP = 'touchend';
        CURRENT_EVENT_OUT = 'touchcancel';
        //return PLATFORM_GAP;
    };
    if (typeof(window) != 'undefined' && window.runtime) {//Air
        return PLATFORM_AIR;
    };
    return PLATFORM_WEB;
};

var ui = {};

var CURRENT_PLATFORM_MOBILE = false;
var CURRENT_EVENT_CLICK = 'mousedown';
var CURRENT_EVENT_DOWN = 'mousedown';
var CURRENT_EVENT_MOVE = 'mousemove';
var CURRENT_EVENT_UP = 'mouseup';
var CURRENT_EVENT_OUT = 'mouseout';
var CURRENT_PLATFORM = detectPlatform();

var log = function() {
    var message = '';
    var dt = new Date();
    if (dt.format) {//Have formatting
        message = dt.format('H:MM:ss')+':';
    };
    var arr = [message];
    for (var i = 0; i < arguments.length; i++) {//Build message
        if (arguments[i] == undefined || arguments[i] == null) {//NULL
            arr.push('[NULL]');
            message += ' [NULL]';
        } else {//Not null
            arr.push(arguments[i]);
            if (arguments[i].toString) {//toString
                message += ''+arguments[i].toString()+' ';
            } else {
                message += ''+arguments[i]+' ';
            };
        };
    };
    if (CURRENT_PLATFORM == PLATFORM_WEB) {//Web - console
        if (window.console) {//Forward call
            if (CURRENT_PLATFORM_MOBILE) {
                window.console.log.call(window.console, message);
            } else {
                window.console.log.apply(window.console, arr);
            }
            return;
        };
    };
    if (CURRENT_PLATFORM == PLATFORM_AIR) {//air.trace
        air.trace.apply(air, arr);
        //air.Introspector.Console.log(message);
        return;
    };
    if (CURRENT_PLATFORM == PLATFORM_TIT) {//air.trace
        Ti.API.info(message);
        return;
    };
};

var getEventCoordinates = function(evt) {//Touch support
    if (CURRENT_PLATFORM_MOBILE && evt && evt.originalEvent && evt.originalEvent.changedTouches && evt.originalEvent.changedTouches[0]) {//
        var x = evt.originalEvent.changedTouches[0].pageX;
        var y = evt.originalEvent.changedTouches[0].pageY;
        return {x: x, y: y};
    } else {//Desktop
        return {x: evt.pageX, y: evt.pageY};
    };
}

var EventEmitter = function(emitter) {//Creates new event emitter
    this.events = {};
    this.emitter = emitter;
};

EventEmitter.prototype.on = function(type, handler, top) {//Adds new handler
    if (!type || !handler) {//Invalid params
        return false;
    };
    var arr = [];
    if (!this.events[type]) {//Add empty array
        this.events[type] = arr;
    } else {//Get array
        arr = this.events[type];
    };
    for (var i = 0; i < arr.length; i++) {//Check for duplicate
        if (arr[i] == handler || (arr[i].marker && arr[i].marker == handler.marker)) {//Already here
            arr[i] = handler;
            return false;
        };
    };
    if (top) {
        arr.splice(0, 0, handler);
    } else {
        arr.push(handler);
    }
    return true;
};

EventEmitter.prototype.off = function(type, handler) {//Removes handler
    if (!type) {//Stop
        return false;
    };
    var arr = this.events[type];
    if (!arr) {//Stop
        return false;
    };
    if (!handler) {//Remove all handlers
        this.events[type] = [];
        return true;
    };
    for (var i = 0; i < arr.length; i++) {//Look for handler
        if (arr[i] == handler || (arr[i].marker && arr[i].marker == handler.marker)) {//Found - splice
            arr.splice(i, 1);
            i--;
        };
    };
    return true;
};

EventEmitter.prototype.emit = function(type, evt, obj) {//Calls handlers
    if (!type) {//Stop
        return false;
    };
    if (!evt) {//Create empty
        evt = {};
    };
    if (!evt.type) {//Add type
        evt.type = type;
    };
    if (!evt.target) {//Add target
        evt.target = obj || this.emitter || this;
    };
    var arr = this.events[type] || [];
    var prevented = false;
    evt.stop = function () {
        prevented = true;
    };
    for (var i = 0; i < arr.length; i++) {//Call handler one by one
        // try {
            var result = arr[i].call(evt.target, evt);
            if (result == false) {//Stop executing
                return false;
            };
            if (prevented) {
                return result;
            };
        // } catch (e) {//Handler error
        //     log('Error in handler:', e);
        // }
    };
    return true;
};

var guidGen = function(items) {//Generates guid {4D0AC2D1-76AC-4a60-BD33-017A0AB33FC9}
    var parts = [8, 4, 4, 4, 12];
    if (!items) {
        items = parts.length;
    };
    var chars = '1234567890abcdef';
    var res = [];
    for (var i = 0; i < items; i++) {
        var s = '';
        for (var j = 0; j < parts[j]; j++) {
            s += chars.charAt(Math.floor(Math.random()*chars.length));
        };
        res.push(s);
    };
    return res.join('-');
};

var dd = {
    hasDDTarget: function(e, type) {
        var evt = e;
        if (e.originalEvent) {
            evt = e.originalEvent;
        };
        //log('hasDDTarget', evt.dataTransfer.types);
        if (!evt.dataTransfer) {
            return false;
        };
        if (!evt.dataTransfer.types) {
            return false;
        };
        //for (var i = 0; i < evt.dataTransfer.types.length; i++) {
            //log('dd', evt.dataTransfer.types[i]);
        //};
        for (var i = 0; i < evt.dataTransfer.types.length; i++) {
            //log('hasDDTarget', evt.dataTransfer.types[i]);
            if (type == evt.dataTransfer.types[i]) {
                return true;
            };
            if ('text/plain' == evt.dataTransfer.types[i]) {
                return true;
            };
        };
        return false;
    },
    getDDTarget: function(e, type) {
        var evt = e;
        if (evt.originalEvent) {
            evt = evt.originalEvent;
        };
        if (!evt.dataTransfer) {
            return null;
        };
        //for (var i = 0; i < evt.dataTransfer.types.length; i++) {
            //log('dd', evt.dataTransfer.types[i], evt.dataTransfer.getData(evt.dataTransfer.types[i]));
        //};
        var data = evt.dataTransfer.getData('text/plain');
        if (data && _.startsWith(data, type+':')) {
            //log('data', data, data.substr(type.length+1));
            return data.substr(type.length+1);
        };
        data = evt.dataTransfer.getData(type);
        if (data) {
            return data;
        };
        return null;
    },
    setDDTarget: function(e, type, value) {
        var evt = e;
        if (evt.originalEvent) {
            evt = evt.originalEvent;
        };
        if (!evt.dataTransfer) {
            return false;
        };
        //evt.dataTransfer.effectAllowed = 'copy';
        evt.dataTransfer.setData('text/plain', ''+type+':'+value);
        return true;
    }
};

var AsyncGrouper = function(count, handler) {//Run async tasks, call handler when it's done
    this.count = count;
    this.handler = handler;
    this.results = [];
    this.statuses = [];
    this.index = 0;
    this.ok = _.bind(this._ok, this);
    this.err = _.bind(this._err, this);
    this.fn = _.bind(this._fn, this);
    this.handlerCalled = false;
};

AsyncGrouper.prototype.check = function() {//Check count status
    if (this.index == this.count && !this.handlerCalled) {//Done
        this.handlerCalled = true;
        this.handler(this);
    };
};

AsyncGrouper.prototype._ok = function() {//OK handler
    this.index ++ ;
    this.statuses.push(true);
    this.results.push(arguments);
    this.check();
};

AsyncGrouper.prototype._fn = function(err, data) {//OK handler
    this.index ++ ;
    if(err) {
        this.statuses.push(false);
        this.results.push([err]);
    } else {
        this.statuses.push(true);
        this.results.push([data]);
    }
    this.check();
};

AsyncGrouper.prototype._err = function() {//Error handler
    this.index ++ ;
    this.statuses.push(false);
    this.results.push(arguments);
    this.check();
};

AsyncGrouper.prototype.findError = function() {//Looks for failed and returns error text
    for (var i = 0; i < this.statuses.length; i++) {
        if (!this.statuses[i]) {//Error
            return this.results[i][0] || 'Unknown error';
        };
    };
    return null;
};

ui.remoteScriptLoader = function (url, object, handler) {
    if (CURRENT_PLATFORM == PLATFORM_AIR) {
        //Special case with sandbox
        log('Loading', url, object);
        var iframe = $(document.createElement('iframe'));
        iframe.width(0).height(0).hide();
        iframe.attr('documentRoot', 'app:/').attr('sandboxRoot', 'http://air/');
        iframe.bind('load', function (event) {
            log('DOM loaded', object);
            var doc = event.target.contentDocument;
            var s = doc.createElement('script');
            s.async = true;
            s.src = url;
            doc.body.appendChild(s);
            var bridge = {
                
            };
            bridge['_'+object] = function () {
                log('Get!', object, _.keys(event.target.contentWindow));
                return event.target.contentWindow[object] || null;
            }
            event.target.contentWindow.childSandboxBridge = bridge;
        })
        iframe.appendTo(document.body);
        // iframe.html('<html><head><script src="'+url+'"></script></head><body></body></html>');
        window['_'+object] = function () {
            log('Get object', iframe.get(0).contentWindow.childSandboxBridge);
            return iframe.get(0).contentWindow[object];
        }
    } else {
        //Use yepnope
        yepnope({
            load: [url],
            complete: function() {
                if (handler) {
                    handler();
                };
            }
        });
        window['_'+object] = function () {
            return window[object];
        };
    }
}