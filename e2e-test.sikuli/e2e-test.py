Settings.MinSimilarity = 0.9
Settings.ObserveScanRate = 3
channelRegion = Region(80,100,250,1000)

def enter():
	sleep(0.3)
	type(Key.ENTER)
	sleep(0.2)
	type(Key.ENTER)

def command(cmdString):
	type(cmdString)
	enter()

def setup():
	switchToEnte()
	cleanChannels()
	createChannel()

def cleanChannels():
	if exists("1664199908377.png", 0.3):
		rightClick(channelRegion.find("1664199908377.png").getTarget().below(25))
		if exists("1677610625687.png", 0.4):
			click("1677610625687.png")
			sleep(0.2)
			click("1677610835291.png")	
	if exists("1658756416028.png", 0.3):
		rightClick(channelRegion.find("1658756416028.png").getTarget().below(25))
		if exists("1677610625687.png", 0.4):
			click("1677610625687.png")
			sleep(0.2)
			click("1677610835291.png")	
	if exists("1658749176570.png", 0.3):
		rightClick(channelRegion.find("1658749176570.png").getTarget().below(25))
		if exists("1677610625687.png", 0.4):
			click("1677610625687.png")
			sleep(0.2)
			click("1677610835291.png")
	rightClick(channelRegion.find("1677611287535.png").getTarget().below(25))
	if exists("1677610625687.png", 0.4):
		click("1677610625687.png")
		sleep(0.2)
		click("1677610835291.png")
		sleep(0.2)
		rightClick(channelRegion.find("1677611287535.png").getTarget().below(25))
		if exists("1677610625687.png", 0.4):
			click("1677610625687.png")
			sleep(0.2)
			click("1677610835291.png")

def createChannel():
	click(Pattern("1677611370325.png").targetOffset(95,-1))
	sleep(0.3)
	type("testchannel\n")
	sleep(1.5)

def deleteRanking():
	sleep(0.1)
	command("/deleteranking testranking")
	sleep(0.5)
	if exists("1656944142796.png"):
		click("1656944142796.png")
		wait("1677612165799.png")

def createRanking():
	type("/createranking" + Key.TAB + "testranking" + Key.TAB + "\n\n")
	sleep(1)

def addQueue():
	while not exists("1677613515191.png"):
		type("/addqueue") 
		sleep(0.1)
		type(Key.ENTER)
		sleep(0.1)
		type(Key.ENTER + "1" + Key.TAB + "2")
		type(Key.TAB + "1v1" + Key.TAB + Key.ENTER + Key.ENTER)
		sleep(1)

def deleteQueue():
	#while not exists(Pattern("1658338286658.png").similar(0.90)):
	command("/deletequeue testranking")
	sleep(0.5)

def join():
	while not exists("1677691992731.png", 0.3) and not exists("1677693174793.png", 0.3):
		command("/join testranking")
		sleep(1)
	if exists("1677692090326.png"):
		click(Pattern("1677692090326.png").targetOffset(48,16))

def leave():
	while not exists("1677692264050.png"):
		type("/leave")
		enter()

def switchToEnte2():
	if exists("1677692156275.png"):
		click(Pattern("1677692156275.png").targetOffset(-91,-3))
		sleep(0.1)
		hover("1677692830691.png")
		mouseMove(200, 0)
		click("1677692851573.png")
		sleep(1)
	gotoTestServer()
	sleep(0.5)


def switchToEnte():
	if exists("1677693547851.png"):
		click(Pattern("1677693547851.png").targetOffset(-53,1))
		sleep(0.1)
		hover("1677693612409.png")
		mouseMove(200, 0)
		click("1677693629377.png")
		sleep(1)
	gotoTestServer()
	sleep(0.5)

def gotoTestServer():
	if not isProductionBot:
		click("1677693676742.png")
	else:
		click("1659467137082.png")

def gotoTestchannel():
	click(find(Pattern("1677611370325.png").targetOffset(95,-1)).below(25))


def reportWin():
	wait(Pattern("1658499573390.png").similar(0.90))
	click(find(Pattern("1658499573390.png").similar(0.90)).left(30))
	click("1658491416051.png")

def reportLoss():
	click(find(Pattern("1658499573390.png").similar(0.90)).left(30))
	click("1658491457637.png")

def reportCancel():
	wait(Pattern("1658499573390.png").similar(0.94))
	click(find(Pattern("1658499573390.png").similar(0.90)).left(30))
	click("1677693792372.png")

def fileDispute():
	wait(Pattern("1658499573390.png").similar(0.94))
	click(find(Pattern("1658499573390.png").similar(0.90)).left(30))
	click("1677693808954.png")
	

def removeNotifications():
	while exists(Pattern("1658499573390.png").similar(0.94)):
		click(find(Pattern("1658499573390.png").similar(0.90)).left(30))

def testPermissions():
	type("/setpermission")
	sleep(0.5)
	type(Key.ENTER)
	sleep(0.1)
	type(Key.ENTER)
	click("1677611645720.png")
	type(Key.ENTER)
	sleep(0.1)
	type("/setpermission\n")
	sleep(0.2)
	type(Key.DOWN)
	type(Key.ENTER)
	click("1677611703519.png")
	type(Key.ENTER)

def testDeleteAndCreateRankingAndQueue():
	deleteQueue()
	deleteRanking()
	if exists("1658858301128.png"):
		for x in range(10):
			type(str(x + 1) + " ")
			sleep(1)
		type(Key.ENTER)
	createRanking()
	addQueue()

def testMisc():
	join()
	command("/queuestatus")
	leave()
	command("/help")
	enter()
	command("/playerinfo")
	
def testWinLose():
	switchToEnte()
	removeNotifications()
	gotoTestchannel()
	join()
	switchToEnte2()
	removeNotifications()
	gotoTestchannel()
	join()
	sleep(3)
	reportWin()
	switchToEnte()
	reportLoss()

def testCancel():
	removeNotifications()
	gotoTestchannel()
	join()
	switchToEnte2()
	removeNotifications()
	gotoTestchannel()
	join()
	sleep(3)
	reportCancel()
	switchToEnte()
	reportCancel()
	sleep(0.8)

def testDisputeRuleAsWin():
	switchToEnte2()
	removeNotifications()
	gotoTestchannel()
	join()
	switchToEnte()
	removeNotifications()
	gotoTestchannel()
	join()
	sleep(3)
	fileDispute()
	sleep(0.5)
	click("1677694540878.png")

def testAddRankAndDeleteRanks():
	gotoTestchannel()
	command("/addrank @Gold\n 1200")
	sleep(2)
	command("/deleteranks")
	
def testSettings():
	gotoTestchannel()
	command("/settings")
	click("1677694769548.png")
	click(Pattern("1677694786881.png").targetOffset(-3,18))
	sleep(0.3)
	click("1677694817229.png")
	click("1677694827847.png")
	sleep(1.2)
	type("1000")
	click("1658853418852.png")
	sleep(0.5)
	click("1677694882627.png")
	sleep(0.3)
	click("1677694892044.png")
	sleep(0.5)
	
def testBan():
	gotoTestchannel()
	type("/ban")
	click("1677694944175.png")
	click("1677694959323.png")
	type("@Ente2\n")
	type("duration:10")
	enter()
	type("/ban")
	click("1677694944175.png")
	click("1677694992871.png")
	type("@Ente2")
	enter()
	
def testForcewin():
	gotoTestchannel()
	command("/forcewin testranking @Ente\n@Ente2")
	
def testRevertmatch():
	channelRegion.click("1677695071295.png")
	rightClick("1677695097885.png")
	hover("1677695112734.png")
	click("1677695125066.png")

def testSetRating():
	gotoTestchannel()
	command("/setrating\n@Ente2\nset\n999")
	
if exists("1665319421164.png", 0.2):
	isProductionBot = True
else:
	isProductionBot = False
setup()
if True:
	testPermissions()	
	testDeleteAndCreateRankingAndQueue()
	testMisc()
	testWinLose()
	testCancel()
	testDisputeRuleAsWin()
	testAddRankAndDeleteRanks()
	testSettings()
	testBan()
	testForcewin()
	testRevertmatch()
	testSetRating()	
	
	
	


