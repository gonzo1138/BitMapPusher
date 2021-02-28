import java.io.*;
import java.util.Scanner;

class BmpFormatException extends Exception {
    public BmpFormatException()
    {
        System.out.println("Kein Windows-Kompatibles BMP-Format. Eingabe-BMP bitte als Windows-24Bit (8R 8G 8B) bereit stellen.");
    }
}

public class BMP {

    private static int convertByteArrayToInt(byte[] data) {            //  3.2 aus https://javadeveloperzone.com/java-basic/java-convert-int-to-byte-array/
        if (data == null || data.length != 4) return 0x0;
        return (
               (0xff & data[0]) << 24  |
               (0xff & data[1]) << 16  |
               (0xff & data[2]) << 8   |
               (0xff & data[3])
        );
    }

    public static int pushValue(int value, int offset){
        int newValue = value+offset;
        if(newValue < 0) return 0;
        else if(newValue > 255) return 255; //return Math.min(newValue, 255);
        return newValue;
    }

    public static void main(String[] args) {

        // Aufrufparameter einlesen:    (Ziel: Verschiebung der RGB-Farbwerte pro Pixel um angegeben Wert)
        int newRed = 0;
        int newGreen = 0;
        int newBlue = 0;
        int luminanz = 0;
        //boolean colorOrLuminanz = false;
        if(args != null) {
            switch (args.length) {
                case 1:
                    luminanz = Integer.parseInt(args[0]);
                    break;
                case 3:
                    newRed = Integer.parseInt(args[0]);
                    newGreen = Integer.parseInt(args[1]);
                    newBlue = Integer.parseInt(args[2]);
                    break;
                default:
                    //System.out.println("Drei Parameter beim Aufruf des Programms übergeben um die Farbkanäle zu verändern.\nEinen Parameter übergeben um die Helligkeit zu verändern.\nZur Strafe für fehlende Parameter wird das Bild nun invertiert. Vielen Dank.");
                    break;
            }
        }

        if (newRed==0 && newGreen==0 && newBlue==0 && luminanz == 0){
            System.out.println("Die folgenden Parameter sind auf einen Wertebereich von 0-255 begrenzt.\nEine Verschiebung von +-128 maximal ist meist sinnvoll.\n");
            Scanner scan = new Scanner(System.in);
            System.out.println("Wie stark soll sich die Helligkeit des Bildes verändern?");
            luminanz = scan.nextInt();
            System.out.println("Um welchen Wert soll Rot verschoben werden?");
            newRed = scan.nextInt();
            System.out.println("Um welchen Wert soll Grün verschoben werden?");
            newGreen = scan.nextInt();
            System.out.println("Um welchen Wert soll Blau verschoben werden?");
            newBlue = scan.nextInt();
        }

        // einlesen und ausgeben der Datei ermöglichen:
        String picSrc = "bosch_garten-der-lueste.bmp";
        String picDst = "output.bmp";

        File picIn  = new File(picSrc);
        File picOut = new File(picDst);
        BufferedInputStream  picStreamIn;
        BufferedOutputStream picStreamOut;

//      https://upload.wikimedia.org/wikipedia/commons/c/c4/BMPfileFormat.png
/*
      BITMAPCOREHEADER        (https://en.wikipedia.org/wiki/BMP_file_format#Bitmap_file_header)
      Offset dec 	Size (bytes)    Purpose
      0             2 bytes 	    identify the BMP and DIB file, 0x42 0x4D in hexadecimal, same as BM in ASCII, BM is for Windows 3.1x, 95, NT, ... etc.
      2 	        4 bytes 	    The size of the BMP file in bytes
      ...
      10            4 bytes     The offset, i.e. starting address, of the byte where the bitmap image data (pixel array) can be found.
      the size of this header: 14 bytes
*/
/*
      BITMAPINFOHEADER        (https://en.wikipedia.org/wiki/BMP_file_format#DIB_header_(bitmap_information_header))
      Offset 	Size (bytes) 	Purpose
      14 	    4            	the size of this header, in bytes (40)
      18 	    4 	            the bitmap width in pixels (signed integer)
      22 	    4 	            the bitmap height in pixels (signed integer)
      26 	    2 	            the number of color planes (must be 1)
      28 	    2 	            the number of bits per pixel, which is the color depth of the image. Typical values are 1, 4, 8, 16, 24 and 32.
      30 	    4 	            the compression method being used. See the next table for a list of possible values
                             -> has to be 0 (BI_RGB) or 11 (BI_CMYK) for uncompressed BMP (so 0 in this case)
      34 	    4 	            the image size. This is the size of the raw bitmap data; a dummy 0 can be given for BI_RGB bitmaps.
      38 	    4 	            the horizontal resolution of the image. (pixel per metre, signed integer)
      42 	    4 	            the vertical resolution of the image. (pixel per metre, signed integer)
      46 	    4 	            the number of colors in the color palette, or 0 to default to 2n
      50 	    4 	            the number of important colors used, or 0 when every color is important; generally ignored
*/

        int[] coreHeader = new int[14];     // Größe bekannt aus Spezifikation
        int[] dibHeader;                    // Größe wird erst ausgelesen
        int[] header;
        int[] image;
        //int[] footer;

        int imageOffset;
        int fileSize;
        int dibHeaderSize;
        int imageSize;

        try{
            picStreamIn  = new BufferedInputStream  (new FileInputStream(picIn));   // throws FileNotFoundException
            picStreamOut = new BufferedOutputStream (new FileOutputStream(picOut)); // throws FileNotFoundException

            // Coreheader-Verarbeitung:
            int coreHeaderCount = 0;                  // Zähler für Stelle im Header (in Bytes)

            // Signaturprüfung
            coreHeader[coreHeaderCount++] = picStreamIn.read();       // hier soll das 'B' eingelesen werden
            coreHeader[coreHeaderCount++] = picStreamIn.read();       // hier soll das 'M' eingelesen werden
            System.out.println("BMP-Format: " + (char) coreHeader[0] + (char) coreHeader[1]);
            if((char) coreHeader[0] != 'B' && (char) coreHeader[1] != 'M') throw new BmpFormatException();  // Liegt mmit "BM" ein Windows-BMP vor?

            // Dateigröße
            byte[] fileSizeBytes = new byte[4];
            for(int i=fileSizeBytes.length-1; i>=0; i--) fileSizeBytes[i] = (byte) picStreamIn.read();
            fileSize = convertByteArrayToInt(fileSizeBytes);        // Konvertiert die 0en und 1en aus 4 Byte in einem Array in eine Zahl einer Integer-Variablen
            System.out.println("Dateigröße: " + fileSize + " bytes");
            for(int i=fileSizeBytes.length-1; i>=0; i--) coreHeader[coreHeaderCount++] = fileSizeBytes[i];     // kopieren der Bytes in coreHeader-Array (Reihenfolge rückwärts wegen Endianess: https://en.wikipedia.org/wiki/Endianness#Example )

            // uninteressanter Zwischenraum
            do coreHeader[coreHeaderCount++] = picStreamIn.read(); while(coreHeaderCount<10);

            // Offset to PixelArray
            byte[] offsetToPixels = new byte[4];
            for(int i=offsetToPixels.length-1; i>=0; i--) offsetToPixels[i] = (byte) picStreamIn.read();
            imageOffset = convertByteArrayToInt(offsetToPixels);
            System.out.println("Image-Offset: " + imageOffset + " bytes");
            for(int i=offsetToPixels.length-1; i>=0; i--) coreHeader[coreHeaderCount++] = offsetToPixels[i];    // kopieren der Bytes in coreHeader-Array
            // ------------------

            // DIB Header
            int dibHeaderCount = 0;

            // DIB Header Size
            byte[] dibHeaderBytes = new byte[4];
            for(int i=dibHeaderBytes.length-1; i>=0; i--) dibHeaderBytes[i] = (byte) picStreamIn.read();
            dibHeaderSize = convertByteArrayToInt(dibHeaderBytes);
            System.out.println("DIB-Header:  " + dibHeaderSize + " bytes");
            for(int i=dibHeaderBytes.length-1; i>=0; i--) coreHeader[dibHeaderCount++] = dibHeaderBytes[i];     // kopieren der Bytes in dibHeader-Array
            dibHeader = new int[dibHeaderSize];

            // Image width

            // Image height

            // ...
            do dibHeader[dibHeaderCount++] = picStreamIn.read(); while(dibHeaderCount<20);

            // Image size
            byte[] imageBytes = new byte[4];
            for(int i=imageBytes.length-1; i>=0; i--) imageBytes[i] = (byte) picStreamIn.read();
            imageSize = convertByteArrayToInt(imageBytes);
            System.out.println("Image-Size:  " + imageSize + " bytes");
            for(int i=imageBytes.length-1; i>=0; i--) dibHeader[dibHeaderCount++] = imageBytes[i];     // kopieren der Bytes in dibHeader-Array

            // restlichen DIB-Header lesen
            do dibHeader[dibHeaderCount++] = picStreamIn.read(); while(dibHeaderCount<dibHeaderSize);

            // kompletten Header in output.bmp schreiben
            int headerCount = 0;
            header = new int[imageOffset];
            for(int i=0; i<coreHeader.length; i++) header[headerCount++] = coreHeader[i];   // Coreheader kopieren
            for(int i=0; i<dibHeader.length; i++) header[headerCount++] = dibHeader[i];     // DIB-Header kopieren
            for(int i=coreHeaderCount+dibHeaderCount; i<imageOffset; i++) header[headerCount++] = picStreamIn.read();  // restlichen Header (Color-Table etc.) aus Datei lesen
            for(int i=0; i<headerCount; i++) picStreamOut.write(header[i]);                 // schreibe Header in Ausgabedatei


            // Pixel des BMP verarbeiten und verändern:
            int byteRead;                                           // Puffer für gelesenes Byte
            int color = 0;
            image = new int[imageSize];     // da Bildanteilgröße aus Header bekannt wird Array dieser Größe erstellt
            for (int i=0; i<image.length; i++){                     // iterieren über die Größe dieses Arrays: lesen, verändern und abspeichern jeweils eines Bytes
                byteRead = picStreamIn.read();

                byteRead = pushValue(byteRead, luminanz);
                switch (color){
                    case 0:
                        byteRead = pushValue(byteRead, newBlue);
                        color++;
                        break;
                    case 1:
                        byteRead = pushValue(byteRead, newGreen);
                        color++;
                        break;
                    case 2:
                        byteRead = pushValue(byteRead, newRed);
                        color=0;
                        break;
                }

                picStreamOut.write(byteRead);
            }

            // Restliche Datei 1:1 übernehmen
            while((byteRead = picStreamIn.read()) != -1){
                picStreamOut.write(byteRead);
            }

/*
            int footerCount = 0;
            footer = new int[fileSize-(imageOffset+imageSize)];
            for(int i=imageOffset+imageSize; i<fileSize; i++) footer[footerCount++] = picStreamIn.read();
*/


            picStreamIn.close();
            picStreamOut.flush();
            picStreamOut.close();
/*
            //Alternative:
            for(int byteRead; (byteRead = picStreamIn.read(image)) != -1;) {
                picStreamOut.write(image, 0, byteRead);   // https://www.programcreek.com/2009/02/java-convert-a-file-to-byte-array-then-convert-byte-array-to-a-file/
            }
*/
        } catch(FileNotFoundException e){
            e.printStackTrace();
            System.out.println("Datei " + picSrc + " konnte nicht geöffnet werden.");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Datei " + picSrc + " konnte nicht gelesen werden.");
        } catch (BmpFormatException e) {
            System.out.println(e.getMessage());
        }


    }
}
