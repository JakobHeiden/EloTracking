Settings.MinSimilarity = 0.9
Settings.ObserveScanRate = 3
channelRegion = Region(80,100,250,1000)

def enter():
	sleep(0.2)
	type(Key.ENTER)
	sleep(0.2)
	type(Key.ENTER)

def command(cmdString):
	type(cmdString)
	enter()

def cleanChannels():
	rightClick(channelRegion.find("1658756416028.png").getTarget().below(25))
	if exists(Pattern("1658492956277.png").similar(0.90), 0.4):
		click("1658492956277.png")
		type(Key.ENTER)
	rightClick(channelRegion.find("1658749176570.png").getTarget().below(25))
	if exists(Pattern("1658492956277.png").similar(0.90), 0.4):
		click("1658492956277.png")
		type(Key.ENTER)
	if exists(Pattern("1658432253622.png").similar(0.75), 0.1):
		numChannelsToDelete = 2
	else: 
		numChannelsToDelete = 0
	for x in range(numChannelsToDelete):
		rightClick(find("1658855388884.png").getTarget().below(25))
		sleep(0.3)
		click(Pattern("1656949343471.png").similar(0.69))
		type(Key.ENTER)
		sleep(0.2)

def deleteRanking():
	sleep(0.1)
	command("/deleteranking testranking")
	sleep(0.5)
	if exists("1656944142796.png"):
		click("1656944142796.png")
		wait("1656949084440.png")

def createChannel():
    click(Pattern("1656953157582.png").targetOffset(95,0))
    sleep(0.3)
    type("testchannel\n")
    sleep(1)

def testPermissions():
	type("/setpermission\n")
	click("1658753288770.png")
	click(Pattern("1656941051735.png").similar(0.90))
	type("\n")
	type("/setpermission\n")
	sleep(0.2)
	click("1656941586792.png")
	click(Pattern("1656941211451.png").similar(0.94))
	type("\n")

def createRanking():
    type("/createranking" + Key.TAB + "testranking" + Key.TAB + "\n\n")
    sleep(1)

def addQueue():
    while not exists(Pattern("1658231877693.png").similar(0.82)):
        type("/addqueue" + Key.ENTER + Key.ENTER + "1" + Key.TAB + "2")
        type(Key.TAB + "1v1" + Key.TAB + Key.ENTER + Key.ENTER)
        sleep(1)

def deleteQueue():
    #while not exists(Pattern("1658338286658.png").similar(0.90)):
	command("/deletequeue testranking")
	sleep(0.5)

def join():
	while not exists("1656952991305.png"):
		command("/join testranking")
		sleep(1)

def leave():
	while not exists(Pattern("1658341665655.png").similar(0.90)):	
		type("/leave")
		enter()

def switchToEnte2():
	if exists(Pattern("1658340107593.png").similar(0.90).targetOffset(-93,2)):
		click(Pattern("1658340107593.png").similar(0.90).targetOffset(-93,2))
		sleep(0.1)
		hover("1658340245588.png")
		click(Pattern("1658340917271.png").similar(0.90))
		sleep(0.7)
	gotoTestServer()
	sleep(1)


def switchToEnte():
	if exists(Pattern("1658340714584.png").similar(0.90).targetOffset(-94,-2)):
		click(Pattern("1658340714584.png").similar(0.90).targetOffset(-94,-2))
		sleep(0.1)
		hover("1658340245588.png")
		mouseMove(200, 0)
		click(Pattern("1658340892108.png").similar(0.90))
		sleep(0.7)
	gotoTestServer()
	sleep(1)

def gotoTestServer():
	click("1658756254043.png")

def gotoTestchannel():
	click(Pattern("1658498059044.png").similar(0.69).targetOffset(6,15))	


def reportWin():
	wait(Pattern("1658499573390.png").similar(0.90))
	click(find(Pattern("1658499573390.png").similar(0.90)).left(30))
	click("1658491416051.png")

def reportLoss():
	click(find(Pattern("1658499573390.png").similar(0.90)).left(30))
	click("1658491457637.png")

def reportCancel():
	wait(Pattern("1658499573390.png").similar(0.90))
	click(find(Pattern("1658499573390.png").similar(0.90)).left(30))
	click("1658750997165.png")

def setup():
	switchToEnte()
	cleanChannels()
	createChannel()

def testDeleteAndCreateRankingAndQueue():
	deleteQueue()
	deleteRanking()
	createRanking()
	addQueue()

def testMisc():
	join()
	type("/queuestatus")
	enter()
	leave()
	type("/help")
	enter()
	type("/playerinfo")
	enter()
	
def testWinLose():
	switchToEnte()
	gotoTestchannel()
	join()
	switchToEnte2()
	gotoTestchannel()
	join()
	sleep(3)
	reportWin()
	switchToEnte()
	reportLoss()

def testCancel():
	gotoTestchannel()
	join()
	switchToEnte2()
	gotoTestchannel()
	join()
	sleep(3)
	reportCancel()
	switchToEnte()
	reportCancel()

def testAddRankAndDeleteRanks():
	gotoTestchannel()
	command("/addrank @Gold\n 1200")
	sleep(2)
	command("/deleteranks")
	
def testSettings():
	gotoTestchannel()
	command("/settings")
	click("1658851858625.png")
	click(Pattern("1658851875874.png").targetOffset(-5,21))
	sleep(0.3)
	click("1658851929626.png")
	click("1658851951271.png")
	wait("1658853366438.png")
	type("1000")
	click("1658853418852.png")
	sleep(0.5)
	click("1658853717062.png")
	sleep(0.3)
	click("1658853729560.png")
	
def testBan():
	gotoTestchannel()
	type("/ban")
	click(Pattern("1658854155846.png").targetOffset(-76,-5))
	click("1658854194498.png")
	type("@Ente2\n")
	type("duration:10")
	enter()
	type("/ban")
	click(Pattern("1658854155846.png").targetOffset(-76,-5))
	click("1658854488751.png")
	type("@Ente2")
	enter()
	
def testForcewin():
	gotoTestchannel()
	command("/forcewin testranking @Ente\n@Ente2")
	
def testRevertmatch():
	channelRegion.click("1658855949366.png")
	rightClick("1658855981102.png")
	hover("1658856018344.png")
	click("1658856034449.png")
	
	

	
	
	


setup()
testPermissions()	
testDeleteAndCreateRankingAndQueue()
testMisc()
testWinLose()
testCancel()
testAddRankAndDeleteRanks()
testSettings()
testBan()
testForcewin()
testRevertmatch()






