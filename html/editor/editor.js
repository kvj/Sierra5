(function() {
  var Editor, EditorProvider, HTMLZeptoEditor, PlainTextEditorProvider,
    __hasProp = Object.prototype.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };

  EditorProvider = (function() {

    EditorProvider.prototype.editor = null;

    function EditorProvider() {}

    EditorProvider.prototype.onSpecialKey = function(key, ctrl, shift, meta) {};

    EditorProvider.prototype.onKey = function(key) {};

    EditorProvider.prototype.getLineCount = function() {
      return 0;
    };

    EditorProvider.prototype.getLine = function(index) {
      return '';
    };

    return EditorProvider;

  })();

  Editor = (function() {

    Editor.prototype.x = 0;

    Editor.prototype.y = 0;

    function Editor(provider) {
      this.provider = provider;
    }

    Editor.prototype.onLineChanged = function(index) {};

    Editor.prototype.onLineAdded = function(index) {};

    Editor.prototype.onLineRemoved = function(index) {};

    Editor.prototype.onCursorMoved = function(x, y) {
      this.x = x;
      this.y = y;
    };

    Editor.prototype.reset = function() {};

    return Editor;

  })();

  PlainTextEditorProvider = (function(_super) {

    __extends(PlainTextEditorProvider, _super);

    function PlainTextEditorProvider(lines) {
      this.lines = lines;
      if (lines.length === 0) this.lines = [''];
      PlainTextEditorProvider.__super__.constructor.call(this);
    }

    PlainTextEditorProvider.prototype.getLineCount = function() {
      return this.lines.length;
    };

    PlainTextEditorProvider.prototype.getLine = function(index) {
      return this.lines[index];
    };

    PlainTextEditorProvider.prototype.onSpecialKey = function(key, ctrl, shift, meta) {
      switch (key) {
        case 13:
          return log('Enter - split lines');
      }
    };

    PlainTextEditorProvider.prototype.onKey = function(key) {
      var line;
      line = this.lines[this.editor.y];
      log('Enter', key, String.fromCharCode(key), line);
      line = line.substr(0, this.editor.x) + String.fromCharCode(key) + line.substr(this.editor.x);
      this.lines[this.editor.y] = line;
      log('Enter', key, String.fromCharCode(key), line);
      this.editor.onLineChanged(this.editor.y);
      return this.editor.onCursorMoved(this.editor.x + 1, this.editor.y);
    };

    return PlainTextEditorProvider;

  })(EditorProvider);

  HTMLZeptoEditor = (function(_super) {

    __extends(HTMLZeptoEditor, _super);

    function HTMLZeptoEditor(provider, root) {
      var _this = this;
      this.provider = provider;
      HTMLZeptoEditor.__super__.constructor.call(this, this.provider);
      this.root = root;
      this.root.attr({});
      this.lines = [];
      this.linesDiv = $(document.createElement('div')).css({
        minWidth: '100%',
        width: 'auto',
        minHeight: '100%'
      }).appendTo(this.root);
      this.linesDiv.on('click', function(event) {
        log('Click on root');
        _this.onCursorMoved(0, _this.lines.length - 1);
        return event.stopPropagation();
      });
      this.cursor = $(document.createElement('div')).css({
        position: 'absolute',
        width: '2px',
        height: '1em',
        backgroundColor: '#aaaaaa'
      }).appendTo(this.root);
      this.tester = $(document.createElement('div')).css({
        position: 'absolute',
        height: '1em',
        left: '0px',
        top: '-1em',
        visibility: 'hidden'
      }).appendTo(this.root);
      this.trap = $(document.createElement('textarea')).attr({
        autocapitalize: 'off',
        autocorrect: 'off'
      }).css({
        position: 'absolute',
        width: '1px',
        padding: '0px',
        margin: '0px',
        height: '1em',
        border: '0px',
        outline: 'none',
        backgroundColor: 'transparent'
      }).appendTo(this.root);
      this.trap.on('keypress', function(event) {
        var _ref;
        if (event.which > 0 && ((_ref = event.which) !== 8 && _ref !== 13)) {
          log('Key:', event.which, event);
          _this.trap.val('');
          return _this.provider.onKey(event.which);
        }
      });
      this.trap.on('keydown', function(event) {
        var _ref;
        if ((_ref = event.keyCode) === 37 || _ref === 39 || _ref === 38 || _ref === 40 || _ref === 13 || _ref === 27 || _ref === 8 || _ref === 9) {
          log('Input:', event.keyCode, event);
          _this.provider.onSpecialKey(event.keyCode, event.ctrlKey, event.shiftKey, event.altKey);
          event.preventDefault();
          event.stopPropagation();
          return false;
        }
      });
    }

    HTMLZeptoEditor.prototype._createLine = function(index) {
      var i, line, result, text, _fn, _ref,
        _this = this;
      text = this.provider.getLine(index);
      line = $(document.createElement('div')).css({
        whiteSpace: 'nowrap',
        height: '1em'
      });
      line.on('click', function(event) {
        _this.onCursorMoved(text.length, index);
        return event.stopPropagation();
      });
      result = {
        div: line,
        chars: []
      };
      _fn = function(i) {
        var ch;
        ch = $(document.createElement('div')).css({
          whiteSpace: 'nowrap',
          display: 'inline'
        }).text(text.charAt(i)).appendTo(line);
        ch.on('click', function(event) {
          _this.onCursorMoved(i, index);
          return event.stopPropagation();
        });
        return result.chars.push(ch);
      };
      for (i = 0, _ref = text.length; 0 <= _ref ? i < _ref : i > _ref; 0 <= _ref ? i++ : i--) {
        _fn(i);
      }
      return result;
    };

    HTMLZeptoEditor.prototype.onLineChanged = function(index) {
      var line;
      line = this.lines[index];
      line.div.remove();
      line = this._createLine(index);
      if (index === 0) {
        this.linesDiv.prepend(line.div);
      } else {
        line.div.insertAfter(this.lines[index - 1].div);
      }
      return this.lines[index] = line;
    };

    HTMLZeptoEditor.prototype.onLineAdded = function(index) {};

    HTMLZeptoEditor.prototype.onLineRemoved = function(index) {};

    HTMLZeptoEditor.prototype.onCursorMoved = function(x, y) {
      this.x = x;
      this.y = y;
      return this._moveCursor();
    };

    HTMLZeptoEditor.prototype.reset = function() {
      var i, line, _i, _len, _ref, _ref2;
      _ref = this.lines;
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        line = _ref[_i];
        line.div.remove();
      }
      this.lines = [];
      for (i = 0, _ref2 = this.provider.getLineCount(); 0 <= _ref2 ? i < _ref2 : i > _ref2; 0 <= _ref2 ? i++ : i--) {
        line = this._createLine(i);
        this.linesDiv.append(line.div);
        this.lines.push(line);
      }
      return this._moveCursor();
    };

    HTMLZeptoEditor.prototype._offset = function(type, item) {
      return item.offset()[type] - this.root.offset()[type] - 1 + this.root.attr(type === 'left' ? 'scrollLeft' : 'scrollTop');
    };

    HTMLZeptoEditor.prototype._moveCursor = function() {
      var cx, cy, line;
      line = this.lines[this.y];
      cy = this._offset('top', line.div);
      if (this.x < line.chars.length) {
        cx = this._offset('left', line.chars[this.x]);
      } else {
        cx = line.chars.length > 0 ? this._offset('left', line.chars[line.chars.length - 1]) + line.chars[line.chars.length - 1].width() : 0;
      }
      log('_moveCursor', this.x, this.y, cx, cy);
      this.cursor.css({
        top: cy,
        left: cx
      });
      this.trap.css({
        top: cy,
        left: cx + 2
      });
      return this.trap.focus();
    };

    return HTMLZeptoEditor;

  })(Editor);

  yepnope({
    load: ['lib/zepto.min.js', 'lib/cross-utils.js', 'editor/editor.css'],
    complete: function() {
      var editor, provider;
      provider = new PlainTextEditorProvider(['Test line1', 'Test line2']);
      editor = new HTMLZeptoEditor(provider, $('#editor'));
      provider.editor = editor;
      log('Started');
      return editor.reset();
    }
  });

}).call(this);
