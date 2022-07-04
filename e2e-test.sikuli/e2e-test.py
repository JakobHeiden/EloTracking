def check_for_fail():
    if exists("1656948317531.png"):
        exit(1)

def deleteChannels():
    if exists("1656949296740.png"):
        rightClick("1656949296740.png")
        click("1656949343471.png")
        click("1656949364365.png")
    if exists("1656949393950.png"):
        rightClick("1656949393950.png")
        click("1656949343471.png")
        click("1656949364365.png")
    if exists(Pattern("1656957021320.png").similar(0.91)):
        rightClick("1656957021320.png")
        click("1656949343471.png")
        click("1656949364365.png") 

def clean():
    type(Key.ENTER + "/deleteranking\n")
    click("1656944024216.png")
    type(Key.ENTER)
    click("1656944142796.png")
    wait("1656949084440.png")
    deleteChannels()
    

click("1656940799893.png")
click(Pattern("1656953157582.png").targetOffset(95,0))
sleep(0.3)
type("testchannel\n")

# deleteChannels()
sleep(0.8)

type("/setpermission\n")
click("1656941551525.png")
click(Pattern("1656941051735.png").similar(0.90))
type("\n")

type("/setpermission\n")
click("1656941586792.png")
click(Pattern("1656941211451.png").similar(0.94))
type("\n")

type("/createranking" + Key.TAB + "testranking" + Key.TAB + "\n\n")
sleep(5)
check_for_fail()

type("/addqueue" + Key.ENTER + Key.ENTER + "1" + Key.TAB + "2")
type(Key.TAB + "1v1" + Key.TAB + Key.ENTER + Key.ENTER)
sleep(5)

type("/join testranking" + Key.ENTER + Key.ENTER)
sleep(0.5)
if not exists("1656952991305.png"):
    clean()
    exit(2)
type("/leave" + Key.ENTER + Key.ENTER)
sleep(0.3)
if not exists("1656953754122.png"):
    clean()
    exit(2)

clean()
check_for_fail()

