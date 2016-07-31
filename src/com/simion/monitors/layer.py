from yowsup.layers.interface                           import YowInterfaceLayer, ProtocolEntityCallback
from yowsup.layers.protocol_messages.protocolentities  import TextMessageProtocolEntity
from yowsup.layers.protocol_receipts.protocolentities  import OutgoingReceiptProtocolEntity
from yowsup.layers.protocol_acks.protocolentities      import OutgoingAckProtocolEntity
from yowsup.layers.protocol_media.protocolentities     import ImageDownloadableMediaMessageProtocolEntity, \
    RequestUploadIqProtocolEntity
from yowsup.layers.network import YowNetworkLayer
#from yowsup.layers import YowLayerEvent
from yowsup.common import YowConstants
from yowsup.layers.protocol_iq.protocolentities     import PingIqProtocolEntity
from yowsup.layers.auth                        import YowAuthenticationProtocolLayer
from yowsup.layers.protocol_media.protocolentities       import *
from yowsup.layers.protocol_media.mediauploader import MediaUploader

import sys, os, time, datetime, mysql.connector, logging

logger = logging.getLogger(__name__)

class EchoLayer(YowInterfaceLayer):
    def __init__(self):
        super(self.__class__, self).__init__()
        self.connected = True

    @ProtocolEntityCallback("success")
    def onSuccess(self, entity):
        print("Logged in!")
        self.connected = True

    @ProtocolEntityCallback("failure")
    def onFailure(self, entity):
        print("Login Failed, reason: %s" % entity.getReason())
        self.connected = False


    def onEvent(self, yowLayerEvent):
        print("event: "+yowLayerEvent.getName())
        if yowLayerEvent.getName() == YowNetworkLayer.EVENT_STATE_DISCONNECTED or \
                yowLayerEvent.getName() == YowNetworkLayer.EVENT_STATE_DISCONNECT:
            self.connected = False
            time.sleep(5)
            sys.exit(0)


    def ping(self):
        if self.connected:
            entity = PingIqProtocolEntity(to = YowConstants.DOMAIN)
            self.toLower(entity)

    def message_send(self, number, content):
        if self.connected:
            outgoingMessage = TextMessageProtocolEntity(content.encode("UTF-8"), to = number)
            self.toLower(outgoingMessage)
            print("Send Text to %s" % number.encode("UTF-8"))


    @ProtocolEntityCallback("message")
    def onMessage(self, messageProtocolEntity):
        if isinstance(messageProtocolEntity, ImageDownloadableMediaMessageProtocolEntity):
            self.onMedia(messageProtocolEntity)
        else:
            self.onText(messageProtocolEntity)

    @ProtocolEntityCallback("receipt")
    def onReceipt(self, entity):
        ack = OutgoingAckProtocolEntity(entity.getId(), "receipt", entity.getType(), entity.getFrom())
        self.toLower(ack)

    @ProtocolEntityCallback("notification")
    def onNotification(self, notification):
        self.output("From :%s, Type: %s" % (self.jidToAlias(notification.getFrom()), notification.getType()), tag = "Notification")
        if self.sendReceipts:
            receipt = OutgoingReceiptProtocolEntity(notification.getId(), notification.getFrom())
            self.toLower(receipt)

    def onText(self, messageProtocolEntity):
        if not messageProtocolEntity.isGroupMessage():
            receipt = OutgoingReceiptProtocolEntity(messageProtocolEntity.getId(), messageProtocolEntity.getFrom())
            self.toLower(receipt)

            #registrar mensagem recebida no BD
            username = self.getStack().getProp(YowAuthenticationProtocolLayer.PROP_CREDENTIALS)[0]
            jid = messageProtocolEntity.getFrom()

            cnx = mysql.connector.connect(user='root', password='', database='zapserver', use_unicode=True)

            cursor = cnx.cursor()
            cursor.execute('SET NAMES utf8mb4')
            cursor.execute("SET CHARACTER SET utf8mb4")
            cursor.execute("SET character_set_connection=utf8mb4")

            cursor.execute("insert into Queue(jidServer, jidClient, url, message, extension, imageLabel, status, dateTime) "
                           "values (%s,%s,%s,convert (%s using utf8mb4),'txt','',%s,%s)",
                                   [username, jid.split('@')[0], jid.split('@')[1], messageProtocolEntity.getBody(),
                                    "R", datetime.datetime.now()])

            cursor.execute("insert into Log(jidServer, jidClient, url, message, extension, status, dateTime)"
                           "values (%s,%s,%s,convert (%s using utf8mb4),'txt',%s,%s)",
                                   [username, jid.split('@')[0], jid.split('@')[1], messageProtocolEntity.getBody(),
                                    "R", datetime.datetime.now()])

            cnx.commit()

            cursor.close()
            cnx.close()

            print("Text %s received from %s" % (messageProtocolEntity.getBody(), messageProtocolEntity.getFrom(False)))

            f = open(username+".log",'a')
            f.write(jid.split('@')[0]+": "+messageProtocolEntity.getBody()+" ("+str(datetime.datetime.now())+")\n")
            f.close()


    def image_send(self, number, imageLabel):
        if self.connected:
            username = self.getStack().getProp(YowAuthenticationProtocolLayer.PROP_CREDENTIALS)[0]

            cnx = mysql.connector.connect(user='root', password='', database='zapserver')
            cursor = cnx.cursor()

            cursor.execute("select mimeType, fileHash, url, ip, size, fileName, encoding, width, height, preview "
                        "from Image where label = %s and jidServidor = %s LIMIT 1", [imageLabel, username])

            for (row0, row1, row2, row3, row4, row5, row6, row7, row8, row9) in cursor:
                outImage = ImageDownloadableMediaMessageProtocolEntity(
                                        str(row0), str(row1), str(row2), str(row3), int(row4), str(row5), str(row6),
                                        int(row7), int(row8), "", to = number, preview = str(row9))

                self.toLower(outImage)

                print("Send ImageLabel to %s" % number)

            cursor.close()



    def onMedia(self, mediaProtocolEntity):
        if mediaProtocolEntity.getMediaType() == "image":
            receipt = OutgoingReceiptProtocolEntity(mediaProtocolEntity.getId(), mediaProtocolEntity.getFrom())

            self.toLower(receipt)

            outImage = ImageDownloadableMediaMessageProtocolEntity(
                mediaProtocolEntity.getMimeType(), mediaProtocolEntity.fileHash, mediaProtocolEntity.url, mediaProtocolEntity.ip,
                mediaProtocolEntity.size, mediaProtocolEntity.fileName, mediaProtocolEntity.encoding,
                mediaProtocolEntity.width, mediaProtocolEntity.height,
                #mediaProtocolEntity.getCaption(),
                "Only text messages ok!!",
                to = mediaProtocolEntity.getFrom(), preview = mediaProtocolEntity.getPreview())

		
            #print("Echoing Image from %s" % mediaProtocolEntity.getFrom(False))
            #print("MimeType: %s" % mediaProtocolEntity.getMimeType())
            #print("FileHash: %s" % mediaProtocolEntity.fileHash)
            #print("url: %s" % mediaProtocolEntity.url)
            #print("ip: %s" % mediaProtocolEntity.ip)
            #print("size: %s" % mediaProtocolEntity.size)
            #print("FileName: %s" % mediaProtocolEntity.fileName)
            #print("encoding: %s" % mediaProtocolEntity.encoding)
            #print("width: %s" % mediaProtocolEntity.width)
            #print("height: %s" % mediaProtocolEntity.height)
            #print("Caption: %s" % str(mediaProtocolEntity.getCaption()))
            #print("Preview: %s" % mediaProtocolEntity.getPreview().encode("UTF-8"))



            #registrar mensagem recebida no BD
            username = self.getStack().getProp(YowAuthenticationProtocolLayer.PROP_CREDENTIALS)[0]
            jid = mediaProtocolEntity.getFrom()


            cnx = mysql.connector.connect(user='root', password='', database='zapserver')

            cursor = cnx.cursor()
            cursor.execute("insert into Image(jidServer, label, mimeType, fileHash, fileName, url, "
                           " ip, size, encoding, width, height, preview) "
                           "values (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s, %s, %s)",
                                   [username, str(mediaProtocolEntity.getCaption()), mediaProtocolEntity.getMimeType(),
                                    mediaProtocolEntity.fileHash, mediaProtocolEntity.fileName, mediaProtocolEntity.url,
                                    mediaProtocolEntity.ip, mediaProtocolEntity.size, mediaProtocolEntity.encoding,
                                    mediaProtocolEntity.width, mediaProtocolEntity.height, mediaProtocolEntity.getPreview().encode("UTF-8")])

            cnx.commit()

            cursor.close()
            cnx.close()



            f = open(username+".log",'a')
            f.write(jid.split('@')[0]+": Image "+str(mediaProtocolEntity.getCaption())+" ("+str(datetime.datetime.now())+")\n")
            f.close()

            
            self.toLower(outImage)



    def getMediaMessageBody(self, message):
        if message.getMediaType() in ("image", "audio", "video"):
            return self.getDownloadableMediaMessageBody(message)
        else:
            return "[Media Type: %s]" % message.getMediaType()


    def getDownloadableMediaMessageBody(self, message):
         return "[Media Type:{media_type}, Size:{media_size}, URL:{media_url}]".format(
            media_type = message.getMediaType(),
            media_size = message.getMediaSize(),
            media_url = message.getMediaUrl()
            )

    ########### image ############

    def image_file_send(self, number, path):
        if self.connected:
            entity = RequestUploadIqProtocolEntity(RequestUploadIqProtocolEntity.MEDIA_TYPE_IMAGE, filePath=path)
            successFn = lambda successEntity, originalEntity: self.onRequestUploadResult(number, path, successEntity, originalEntity)
            errorFn = lambda errorEntity, originalEntity: self.onRequestUploadError(number, path, errorEntity, originalEntity)

            self._sendIq(entity, successFn, errorFn)

    def doSendImage(self, filePath, url, to, ip = None):
        entity = ImageDownloadableMediaMessageProtocolEntity.fromFilePath(filePath, url, ip, to)
        self.toLower(entity)

    def onRequestUploadResult(self, jid, filePath, resultRequestUploadIqProtocolEntity, requestUploadIqProtocolEntity):
        if resultRequestUploadIqProtocolEntity.isDuplicate():
            self.doSendImage(filePath, resultRequestUploadIqProtocolEntity.getUrl(), jid,
                             resultRequestUploadIqProtocolEntity.getIp())
        else:
            # successFn = lambda filePath, jid, url: self.onUploadSuccess(filePath, jid, url, resultRequestUploadIqProtocolEntity.getIp())
            mediaUploader = MediaUploader(jid, self.getOwnJid(), filePath,
                                      resultRequestUploadIqProtocolEntity.getUrl(),
                                      resultRequestUploadIqProtocolEntity.getResumeOffset(),
                                      self.onUploadSuccess, self.onUploadError, self.onUploadProgress, async=False)
            mediaUploader.start()

    def onRequestUploadError(self, jid, path, errorRequestUploadIqProtocolEntity, requestUploadIqProtocolEntity):
        logger.error("Request upload for file %s for %s failed" % (path, jid))

    def onUploadSuccess(self, filePath, jid, url):
        self.doSendImage(filePath, url, jid)

    def onUploadError(self, filePath, jid, url):
        logger.error("Upload file %s to %s for %s failed!" % (filePath, url, jid))

    def onUploadProgress(self, filePath, jid, url, progress):
        sys.stdout.write("%s => %s, %d%% \r" % (os.path.basename(filePath), jid, progress))
        sys.stdout.flush()


     ########### audio ############


    def audio_send(self, number, path):
        if self.connected:
            entity = RequestUploadIqProtocolEntity(RequestUploadIqProtocolEntity.MEDIA_TYPE_AUDIO, filePath=path)
            successFn = lambda successEntity, originalEntity: self.onRequestUploadResultAudio(number, path, successEntity, originalEntity)
            errorFn = lambda errorEntity, originalEntity: self.onRequestUploadError(number, path, errorEntity, originalEntity)

            self._sendIq(entity, successFn, errorFn)

    def doSendMedia(self, filePath, url, mediaType, to, ip = None):
        entity = DownloadableMediaMessageProtocolEntity.fromFilePath(filePath, url, mediaType, ip, to)
        self.toLower(entity)

    def onRequestUploadResultAudio(self, jid, filePath, resultRequestUploadIqProtocolEntity, requestUploadIqProtocolEntity):
        if resultRequestUploadIqProtocolEntity.isDuplicate():
            self.doSendMedia(filePath, resultRequestUploadIqProtocolEntity.getUrl(), "audio", jid,
                             resultRequestUploadIqProtocolEntity.getIp())
        else:
            mediaUploader = MediaUploader(jid, self.getOwnJid(), filePath,
                                      resultRequestUploadIqProtocolEntity.getUrl(),
                                      resultRequestUploadIqProtocolEntity.getResumeOffset(),
                                      self.onUploadSuccessAudio, self.onUploadError, self.onUploadProgress, async=False)
            mediaUploader.start()


    def onUploadSuccessAudio(self, filePath, jid, url):
        self.doSendMedia(filePath, url, "audio", jid)


     ########### video ############

    def video_send(self, number, path):
        if self.connected:
            entity = RequestUploadIqProtocolEntity(RequestUploadIqProtocolEntity.MEDIA_TYPE_VIDEO, filePath=path)
            successFn = lambda successEntity, originalEntity: self.onRequestUploadResultVideo(number, path, successEntity, originalEntity)
            errorFn = lambda errorEntity, originalEntity: self.onRequestUploadError(number, path, errorEntity, originalEntity)

            self._sendIq(entity, successFn, errorFn)

    def onRequestUploadResultVideo(self, jid, filePath, resultRequestUploadIqProtocolEntity, requestUploadIqProtocolEntity):
        if resultRequestUploadIqProtocolEntity.isDuplicate():
            self.doSendMedia(filePath, resultRequestUploadIqProtocolEntity.getUrl(), "video", jid,
                             resultRequestUploadIqProtocolEntity.getIp())
        else:
            mediaUploader = MediaUploader(jid, self.getOwnJid(), filePath,
                                      resultRequestUploadIqProtocolEntity.getUrl(),
                                      resultRequestUploadIqProtocolEntity.getResumeOffset(),
                                      self.onUploadSuccessVideo, self.onUploadError, self.onUploadProgress, async=False)
            mediaUploader.start()


    def onUploadSuccessVideo(self, filePath, jid, url):
        self.doSendMedia(filePath, url, "video", jid)