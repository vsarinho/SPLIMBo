

from layer import EchoLayer
from yowsup.layers.auth                        import YowAuthenticationProtocolLayer
from yowsup.layers.auth                        import AuthError
from yowsup.layers.protocol_messages           import YowMessagesProtocolLayer
from yowsup.layers.protocol_media              import YowMediaProtocolLayer
from yowsup.layers.protocol_receipts           import YowReceiptProtocolLayer
from yowsup.layers.protocol_acks               import YowAckProtocolLayer
from yowsup.layers.network                     import YowNetworkLayer
from yowsup.layers.coder                       import YowCoderLayer

from yowsup.layers.auth                        import YowCryptLayer
from yowsup.layers.stanzaregulator             import YowStanzaRegulator
from yowsup.layers.logger                      import YowLoggerLayer
from yowsup.layers.axolotl                     import YowAxolotlLayer

from yowsup.stacks import YowStack
from yowsup.common import YowConstants
from yowsup.layers import YowLayerEvent
from yowsup.stacks import YowStack, YOWSUP_CORE_LAYERS
import os, sys, time, mysql.connector, threading
from yowsup import env
from random import randint
import logging
logging.basicConfig(level=logging.DEBUG)

CREDENTIALS = (sys.argv[2], sys.argv[3]) # replace with your phone and password


class MyThread(threading.Thread):
    def __init__(self,stack):
        super(MyThread, self).__init__()

        self.echoLayer = None
        self.exit = False

        counter = 0

        while (self.echoLayer is None):
            if isinstance(stack.getLayer(counter), EchoLayer):
                self.echoLayer = stack.getLayer(counter)
            counter +=1

    def finish(self):
        self.exit = True

    def run(self):
         print("Starting Server-"+sys.argv[1])
         time.sleep(5)
         print("Monitoring ...")

         while (self.echoLayer.connected and not self.exit):
             self.monitoring()
             time.sleep(2)




    def monitoring(self):
        status = "Conected: "+str(self.echoLayer.connected)

        print("Server-"+sys.argv[1]+" => "+time.ctime() + ", " + status)

        #verificar mensagens do BD para enviar
        cnx = mysql.connector.connect(user='root', password='', database='zapserver', use_unicode=True)

        cursor = cnx.cursor()
        cursor.execute('SET NAMES utf8mb4')
        cursor.execute("SET CHARACTER SET utf8mb4")
        cursor.execute("SET character_set_connection=utf8mb4")

        cursor.execute("select id, jidServer, jidClient, message, data, length(data), url, extension, imageLabel "
                    "from Queue where (dateTimetoSend < now() or dateTimetoSend is null) and status = 'S' "
                    "and jidServer = %s and url like 'whatsapp' LIMIT 1", [CREDENTIALS[0]])

        currentIDs = []

        for (row0, row1, row2, row3, row4, row5, row6, row7, row8) in cursor:
            if row8 is None:
                print("Text Message "+str(row0))
                try:
                    self.echoLayer.message_send(str(row2)+"@"+str(row6), row3.encode('utf-8', 'ignore').decode('utf-8')).ljust(randint(0,9))

                except:
                    e = sys.exc_info()[0]
                    print("Error: %s" % e)
                
                currentIDs += [str(row0)]
            else:
                print("Extension: "+str(row7))
                if row7 == "mp4" or row7 == "avi":
                    print("Video by Path "+str(row8.encode('ascii', 'ignore').decode('ascii')))
                    #self.echoLayer.image_send(str(row2)+"@"+str(row6), str(row8))
                    try:
                        self.echoLayer.video_send(str(row2)+"@"+str(row6), str(row8.encode('ascii', 'ignore').decode('ascii')))
                    except:
                        e = sys.exc_info()[0]
                        print("Error: %s" % e)

                elif row7 == "mp3":
                    print("Audio by Path "+str(row8.encode('ascii', 'ignore').decode('ascii')))
                    #self.echoLayer.image_send(str(row2)+"@"+str(row6), str(row8))
                    try:
                        self.echoLayer.audio_send(str(row2)+"@"+str(row6), str(row8.encode('ascii', 'ignore').decode('ascii')))
                    except:
                        e = sys.exc_info()[0]
                        print("Error: %s" % e)

                else:
                    print("Image by Path "+str(row8.encode('ascii', 'ignore').decode('ascii')))
                    #self.echoLayer.image_send(str(row2)+"@"+str(row6), str(row8))
                    try:
                        self.echoLayer.image_file_send(str(row2)+"@"+str(row6), str(row8.encode('ascii', 'ignore').decode('ascii')))
                    except:
                        e = sys.exc_info()[0]
                        print("Error: %s" % e)
		    
                currentIDs += [str(row0)]

        for id in currentIDs:
            cursor.execute("DELETE FROM Queue where id = %s",[id])
            print("Removing "+id)

        cnx.commit()
        cursor.close()




def main():
    layers = (
                EchoLayer,
                (YowAuthenticationProtocolLayer, YowMessagesProtocolLayer, YowReceiptProtocolLayer, YowAckProtocolLayer, YowMediaProtocolLayer),
                YowAxolotlLayer,
                YowLoggerLayer,
                YowCoderLayer,
                YowCryptLayer,
                YowStanzaRegulator,
                YowNetworkLayer
            )


    thread = MyThread(YowStack(layers))
    try:
        print ("Starting "+sys.argv[1])
        stack = YowStack(layers)

        stack.setProp(YowAuthenticationProtocolLayer.PROP_CREDENTIALS, CREDENTIALS)         #setting credentials
        stack.setProp(YowNetworkLayer.PROP_ENDPOINT, YowConstants.ENDPOINTS[0])    #whatsapp server address
        stack.setProp(YowCoderLayer.PROP_DOMAIN, YowConstants.DOMAIN)
        stack.setProp(YowCoderLayer.PROP_RESOURCE, env.CURRENT_ENV.getResource())          #info about us as WhatsApp client

        stack.broadcastEvent(YowLayerEvent(YowNetworkLayer.EVENT_STATE_CONNECT))   #sending the connect signal

        thread = MyThread(stack)
        thread.start()
        stack.loop(timeout = 0.5, discrete = 0.5)
        print("Exit "+sys.argv[1]+"!!")

    except Exception as e:
        print("Error in "+sys.argv[1]+", reason %s" % e)

    finally:
        thread.finish()
        time.sleep(5)
        print("Restart "+sys.argv[1]+"!!")
        #os.system("python run-Executi.py")
        sys.exit(0)



if __name__ == "__main__":
    main()
