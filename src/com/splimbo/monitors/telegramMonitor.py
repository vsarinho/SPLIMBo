import pprint, telepot, sys, time, threading, mysql.connector #pymysql.cursors


class MyThread(threading.Thread):
    def __init__(self,bot):
        super(MyThread, self).__init__()

        self.bot = bot
        self.exit = False
        self.counter = 0

    def finish(self):
        self.exit = True

    def run(self):
         while (not self.exit):
             self.monitoring()
             time.sleep(0.5)

    def monitoring(self):
        if self.counter > 40:
            print("Server-"+sys.argv[1]+" => "+time.ctime())
            self.counter = 0;

        self.counter = self.counter + 1

        #verificar mensagens do BD para enviar
        cnx = mysql.connector.connect(user='root', password='root', database='zapserver', use_unicode=True)
        # cnx = pymysql.connect(host='localhost', user='root', password='root', db='zapserver', charset='utf8mb4', cursorclass=pymysql.cursors.DictCursor)

        cursor = cnx.cursor()
        cursor.execute('SET NAMES utf8mb4')
        cursor.execute("SET CHARACTER SET utf8mb4")
        cursor.execute("SET character_set_connection=utf8mb4")

        cursor.execute("select id, jidServer, jidClient, message, data, length(data), url, extension, imageLabel "
                    "from Queue where (dateTimetoSend < now() or dateTimetoSend is null) and status = 'S' "
                    "and jidServer = '"+jidServer+"' and url = 'telegram' LIMIT 10")

        currentIDs = []

        for (row0, row1, row2, row3, row4, row5, row6, row7, row8) in cursor:
            if row8 is None:
                print("Text Message "+str(row0))
                try:
                    self.bot.sendMessage(str(row2), row3.encode('utf-8', 'ignore').decode('utf-8'))
                except:
                    e = sys.exc_info()[0]
                    print("Error: %s" % e)

                currentIDs += [str(row0)]
            else:
                print("Extension: "+str(row7))
                if row7 == "mp4" or row7 == "avi":
                    print("Video by Path "+str(row8.encode('ascii', 'ignore').decode('ascii')))

                    try:
                        bot.sendVideo(str(row2), open(str(row8.encode('ascii', 'ignore').decode('ascii')), 'rb'))
                    except:
                        e = sys.exc_info()[0]
                        print("Error: %s" % e)

                elif row7 == "mp3" :
                    print("Audio by Path "+str(row8.encode('ascii', 'ignore').decode('ascii')))

                    try:
                        bot.sendAudio(str(row2), open(str(row8.encode('ascii', 'ignore').decode('ascii')), 'rb'))
                    except:
                        e = sys.exc_info()[0]
                        print("Error: %s" % e)
                elif row7 == "ogg":
                    print("Voice by Path "+str(row8.encode('ascii', 'ignore').decode('ascii')))

                    try:
                        bot.sendVoice(str(row2), open(str(row8.encode('ascii', 'ignore').decode('ascii')), 'rb'))
                    except:
                        e = sys.exc_info()[0]
                        print("Error: %s" % e)

                else:
                    print("Image by Path "+str(row8.encode('ascii', 'ignore').decode('ascii')))

                    try:
                        bot.sendPhoto(str(row2), open(str(row8.encode('ascii', 'ignore').decode('ascii')), 'rb'))
                    except:
                        e = sys.exc_info()[0]
                        print("Error: %s" % e)

                currentIDs += [str(row0)]

        for id in currentIDs:
            cursor.execute("DELETE FROM Queue where id = %s",[id])
            print("Removing "+id)

        cnx.commit()
        cursor.close()



def handle(msg):
    pprint.pprint(msg)
    #print("Chegou mensagem!!");

    message = None
    extension = None
    imageLabel = None

    if 'text' in msg:
        message = msg['text'].encode('ascii', 'ignore').decode('ascii')
        extension = 'txt'

    elif 'location' in msg:
        message = str(msg['location']['latitude'])+","+str(msg['location']['longitude'])
        extension = 'loc'

    elif ('photo' in msg):
        bot.download_file(msg['photo'][1]['file_id'], str(msg['from']['id']))
        extension = 'jpg'
        imageLabel = msg['from']['id']

    elif ('video' in msg):
        bot.download_file(msg['video']['file_id'], str(msg['from']['id']))
        extension = 'mp4'
        imageLabel = msg['from']['id']

    elif ('voice' in msg):
        bot.download_file(msg['voice']['file_id'], str(msg['from']['id']))
        extension = 'ogg'
        imageLabel = msg['from']['id']

    elif ('audio' in msg):
        bot.download_file(msg['audio']['file_id'], str(msg['from']['id']))
        extension = 'mp3'
        imageLabel = msg['from']['id']


    if extension is not None:
        cnx = mysql.connector.connect(user='root', password='root', database='zapserver', use_unicode=True)
        #cnx = pymysql.connect(host='localhost', user='root', password='root', db='zapserver', charset='utf8mb4', cursorclass=pymysql.cursors.DictCursor)

        cursor = cnx.cursor()
        cursor.execute('SET NAMES utf8mb4')
        cursor.execute("SET CHARACTER SET utf8mb4")
        cursor.execute("SET character_set_connection=utf8mb4")

        cursor.execute("insert into Queue(jidServer, jidClient, url, message, extension, imageLabel, status, dateTime) "
                   "values (%s,%s,'telegram',%s,%s,%s,'R',now())",
                   [jidServer, msg['from']['id'], message, extension, imageLabel])

        cursor.execute("insert into Log(jidServer, jidClient, url, message, extension, status, dateTime) "
                   "values (%s,%s,'telegram',%s,%s,'R',now())",
                   [jidServer, msg['from']['id'], message, extension])

        cnx.commit()

        cursor.close()
        cnx.close()


# Getting the token from command-line is better than embedding it in code,
# because tokens are supposed to be kept secret.
TOKEN = sys.argv[3]
jidServer = sys.argv[2]
print ('Starting jidServer:'+jidServer+' token:'+TOKEN)

bot = telepot.Bot(jidServer+':'+TOKEN)
bot.getMe()
bot.message_loop(handle)

thread = MyThread(bot)
thread.start()

# Keep the program running.
while 1:
    time.sleep(10)
