class EditorProvider

    editor: null

    constructor: ->

    onSpecialKey: (key, ctrl, shift, meta) ->

    onKey: (key) ->

    getLineCount: () ->
        0

    getLine: (index) ->
        ''

class Editor

    x: 0
    y: 0

    constructor: (@provider) ->

    onLineChanged: (index) ->
    onLineAdded: (index) ->
    onLineRemoved: (index) ->

    onCursorMoved: (@x, @y) ->

    reset: ->

class PlainTextEditorProvider extends EditorProvider

    constructor: (lines) ->
        @lines = lines
        if lines.length is 0
            @lines = ['']
        super()

    getLineCount: () ->
        @lines.length

    getLine: (index) ->
        @lines[index]

    onSpecialKey: (key, ctrl, shift, meta) ->
        switch key
            when 13
                log 'Enter - split lines'

    onKey: (key) ->
        line = @lines[@editor.y]
        log 'Enter', key, String.fromCharCode(key), line
        line = line.substr(0, @editor.x)+String.fromCharCode(key)+line.substr(@editor.x)
        @lines[@editor.y] = line
        log 'Enter', key, String.fromCharCode(key), line
        @editor.onLineChanged(@editor.y)
        @editor.onCursorMoved @editor.x+1, @editor.y


class HTMLZeptoEditor extends Editor

    constructor: (@provider, root) ->
        super(@provider)
        @root = root
        @root.attr({
            # tabindex: 0
        })
        @lines = []
        @linesDiv = $(document.createElement('div')).css({
            minWidth: '100%'
            width: 'auto'
            minHeight: '100%'
        }).appendTo(@root)
        @linesDiv.on 'click', (event) =>
            log 'Click on root'
            @onCursorMoved(0, @lines.length-1)
            event.stopPropagation()
        @cursor = $(document.createElement('div')).css({
            position: 'absolute'
            width: '2px'
            height: '1em'
            backgroundColor: '#aaaaaa'
        }).appendTo(@root)
        @tester = $(document.createElement('div')).css({
            position: 'absolute'
            height: '1em'
            left: '0px'
            top: '-1em'
            visibility: 'hidden'
        }).appendTo(@root)
        @trap = $(document.createElement('textarea')).attr({
            autocapitalize: 'off'
            autocorrect: 'off'
        }).css({
            position: 'absolute'
            width: '1px'
            padding: '0px'
            margin: '0px'
            height: '1em'
            border: '0px'
            outline: 'none'
            backgroundColor: 'transparent'
        }).appendTo(@root)
        @trap.on 'keypress', (event) =>
            if event.which > 0 and event.which not in [8, 13]
                log 'Key:', event.which, event
                @trap.val ''
                @provider.onKey event.which
        @trap.on 'keydown', (event) =>
            if event.keyCode in [37, 39, 38, 40, 13, 27, 8, 9]
                log 'Input:', event.keyCode, event
                @provider.onSpecialKey event.keyCode, event.ctrlKey, event.shiftKey, event.altKey
                event.preventDefault()
                event.stopPropagation()
                return false
            # @trap.val ''

    _createLine: (index) ->
        text = @provider.getLine index
        line = $(document.createElement('div')).css({
            # width: '100%'
            whiteSpace: 'nowrap'
            height: '1em'
        })
        line.on 'click', (event) =>
            # log 'Click on line', index
            @onCursorMoved(text.length, index)
            event.stopPropagation()
        result = {
            div: line
            chars: []
        }
        for i in [0...text.length]
            do (i) =>
                ch = $(document.createElement('div')).css({
                    whiteSpace: 'nowrap'
                    display: 'inline'
                }).text(text.charAt(i)).appendTo(line)
                ch.on 'click', (event) =>
                    # log 'Click on char', i, index
                    @onCursorMoved(i, index)
                    event.stopPropagation()
                result.chars.push ch
        return result

    onLineChanged: (index) ->
        line = @lines[index]
        line.div.remove()
        line = @_createLine index
        if index is 0
            @linesDiv.prepend line.div
        else
            line.div.insertAfter(@lines[index-1].div)
        @lines[index] = line

    onLineAdded: (index) ->
    onLineRemoved: (index) ->

    onCursorMoved: (@x, @y) ->
        @_moveCursor()

    reset: ->
        for line in @lines # remove all lines
            line.div.remove()
        @lines = []
        for i in [0...@provider.getLineCount()]
            line = @_createLine i
            @linesDiv.append line.div
            @lines.push line
        @_moveCursor()

    _offset: (type, item) ->
        item.offset()[type] - @root.offset()[type] - 1 + @root.attr(if type is 'left' then 'scrollLeft' else 'scrollTop')

    _moveCursor: () ->
        # moves trap and cursor to x, y
        line = @lines[@y]
        cy = @_offset('top', line.div) # border
        if @x<line.chars.length
            cx = @_offset('left', line.chars[@x])
        else
            cx = if line.chars.length>0
                    @_offset('left', line.chars[line.chars.length-1])+line.chars[line.chars.length-1].width()
                else
                    0
        log '_moveCursor', @x, @y, cx, cy
        @cursor.css({
            top: cy
            left: cx
        })
        @trap.css({
            top: cy
            left: cx+2
        })
        @trap.focus()


yepnope {
    load: ['lib/zepto.min.js', 'lib/cross-utils.js', 'editor/editor.css']
    complete: () ->
        provider = new PlainTextEditorProvider ['Test line1', 'Test line2']
        editor = new HTMLZeptoEditor provider, $('#editor')
        provider.editor = editor
        log 'Started'
        editor.reset()
}