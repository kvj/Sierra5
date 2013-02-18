import sublime, sublime_plugin, re, os, threading, subprocess

collapseRe = re.compile('^(\s*)(.*?)(\/-)?\s*$')
lineRe = re.compile('^(\s*)(.*?)\s*$')

# Returns indent level in spaces
def getLevel(view, line):
	line = view.line(view.text_point(line, 0))
	m = lineRe.match(view.substr(line))
	return len(m.group(1))

# Returns pair or rows (start line, end line) according to indent
def getBlock(view, line):
	left = getLevel(view, line)
	row = line+1
	point = view.text_point(row, 0)
	while point<view.size():
		_left = getLevel(view, row)
		if _left<=left:
			break
		row = row+1
		point = view.text_point(row, 0)
	return (line, row-1)

def collapseFrom(view, fromLine):
	point = 0
	row = 0
	level = -1
	minLevel = -1
	if fromLine >= 0:
		minLevel = getLevel(view, fromLine)
		row = fromLine
	levels = []
	while point<view.size():
		point = view.text_point(row, 0)
		line = view.line(point)
		m = collapseRe.match(view.substr(line))
		left = len(m.group(1))
		if left<=level:
			pop = None
			while len(levels)>0:
				pop = levels.pop()
				if pop[2] and pop[3]<row-1:
					result = view.fold(sublime.Region(pop[1].b, point-1))
					# print 'Collapse', pop, point, 'from', pop[3], 'to', row, result
				if pop[0] <= left: break
			if minLevel>=left:
				return
		levels.append((left, line, m.group(3), row))
		level = left
		# print 'Line', point, row, line, m.group(1, 2, 3), left
		row = row+1

class S5CollapseListener(sublime_plugin.EventListener):
	"""Collapses loaded text according to /- macro and file configuration"""
	def on_load(self, view):
		if view.scope_name(0).find('text.s5') == -1:
			return
		print 'Loaded...', view.file_name()
		collapseFrom(view, -1)
		

compileRe = re.compile('^(\s*)\[\[(.+)\]\] #begin( ([a-z]+)(.*))?$')

def showError(text):
	def cb():
		sublime.error_message(text)
	sublime.set_timeout(cb, 0)

def showMessage(text):
	def cb():
		sublime.status_message(text)
	sublime.set_timeout(cb, 0)

class S5CompileBlockCommand(sublime_plugin.TextCommand):
	def run(self, edit):
		view = self.view
		(line, col) = self.view.rowcol(self.view.sel()[0].a) # Line where we are
		check = line
		header = None
		while check>=0: # Search from selected line up for #begin macro
			lineText = view.substr(view.line(view.text_point(check, 0)))
			m = compileRe.match(lineText)
			if m:
				header = m
				break
			check = check-1
		if not header or not header.group(4):
			# Not found
			sublime.error_message('Header not found')
			return
		print 'Header:', header.group(2, 3, 4, 5)
		config = view.settings().get('s5_compile_block', {})
		saveTo = header.group(2)
		compileType = header.group(4)
		compileConfig = config[compileType]
		if not compileConfig:
			sublime.error_message('Not supported type: '+compileType)
			return
		fileName = view.file_name()
		fileFolder = os.path.dirname(fileName)
		saveToFile = os.path.normpath(os.path.join(fileFolder, saveTo))
		saveToFolder = os.path.dirname(saveToFile)
		if not os.path.exists(saveToFolder):
			print 'Creating folder:', saveToFolder
			try:
				os.makedirs(saveToFolder)
			except Exception, e:
				print 'Error:', e
				sublime.error_message('Error creating folder: '+saveToFolder)
				return
		print 'File', fileName, fileFolder, saveTo, saveToFile
		contents = u''
		(blockStart, blockEnd) = getBlock(view, check)
		blockStart = blockStart+1
		padding = getLevel(view, blockStart)
		while blockStart<blockEnd:
			text = view.substr(view.line(view.text_point(blockStart, 0)))[padding:]
			if '#end' == text:
				break
			contents = contents+text+'\n'
			blockStart = blockStart+1
		def doCompile():
			try:
				outFile = open(saveToFile, 'w+')
				p = subprocess.Popen(compileConfig['command'], stdout=outFile, stdin=subprocess.PIPE, shell=True)
				p.communicate(input = contents.encode('utf8'))
				print 'Exec result:', p.returncode
				if 0 != p.returncode:
					raise Exception('Failed to execute')
				showMessage('Compilation finished')
				def cb():
					openLink(view, check)
				sublime.set_timeout(cb, 0)
			except Exception, e:
				print 'Error executing:', e
				showError('Error compiling block')
		thread = threading.Thread(None, doCompile).start()

linkRe = re.compile('^\s*\[\[(.+)\]\].*$')

def findLink(view, fromLine):
	(startLine, endLine) = getBlock(view, fromLine)
	linkFile = None
	# print 'openLink', startLine, endLine
	while startLine<=endLine:
		lineText = view.substr(view.line(view.text_point(startLine, 0)))
		m = linkRe.match(lineText)
		print 'Line', lineText, m
		if m:
			linkFile = m.group(1)
			break
		startLine = startLine+1
	return linkFile

def deleteLink(view, fromLine):
	linkFile = findLink(view, fromLine)
	if not linkFile:
		sublime.error_message('Link not found in block')
		return False
	fileName = view.file_name()
	fileFolder = os.path.dirname(fileName)
	targetFile = os.path.normpath(os.path.join(fileFolder, linkFile))
	if not os.path.exists(targetFile):
		sublime.status_message('File not found')
		return False
	if sublime.ok_cancel_dialog('Delete file ['+linkFile+']?', 'Delete'):
		try:
			os.remove(targetFile)
			targetFolder = os.path.dirname(targetFile)
			files = os.listdir(targetFolder)
			if len(files) == 0:
				os.rmdir(targetFolder)
		except Exception, e:
			print 'deleteLink Error', e, targetFile
			sublime.error_message('Error removing file')

def openLink(view, fromLine):
	linkFile = findLink(view, fromLine)
	if not linkFile:
		sublime.error_message('Link not found in block')
		return False
	fileName = view.file_name()
	fileFolder = os.path.dirname(fileName)
	openFile = os.path.normpath(os.path.join(fileFolder, linkFile))
	try:
		os.startfile(openFile)
		return True
	except Exception, e:
		print 'Error', e, openFile
		sublime.error_message('Error opening link: '+linkFile)
		return False

class S5OpenLinkCommand(sublime_plugin.TextCommand):
	def run(self, edit):
		view = self.view
		(line, col) = self.view.rowcol(self.view.sel()[0].a) # Line where we are
		openLink(view, line)

class S5DeleteLinkCommand(sublime_plugin.TextCommand):
	def run(self, edit):
		view = self.view
		(line, col) = self.view.rowcol(self.view.sel()[0].a) # Line where we are
		deleteLink(view, line)

class S5ToggleFoldCommand(sublime_plugin.TextCommand):
	def run(self, edit):
		(line, col) = self.view.rowcol(self.view.sel()[0].a)
		(a, b) = getBlock(self.view, line)
		# print 'Toggle fold', line, col, a, b		
		if b>=a+1:
			# Have more than one line
			region = sublime.Region(self.view.line(self.view.text_point(a, 0)).b, self.view.line(self.view.text_point(b, 0)).b)
			foldResult = self.view.fold(region)
			# print 'Fold', region, foldResult
			if not foldResult:
				# Already folded - unfold
				self.view.unfold(region)