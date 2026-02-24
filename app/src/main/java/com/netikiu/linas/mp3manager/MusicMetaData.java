package com.netikiu.linas.mp3manager;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagField;


public class MusicMetaData {

    public static class F {
        String title;
        String artist;
        int rating;

        File f;
    }


    public static F extractData(File musicFile) throws Exception {

        F ff = new F();


        ff.f = musicFile;
//        ff.from = musicFile.getPath();
        ff.rating = -1;

        AudioFile f = null;

        // if the file cannot be processed
        try {
            f = AudioFileIO.read(musicFile);
        } catch (Throwable t) {
            System.out.println("File cannot be processed: " + musicFile);
            t.printStackTrace();
            return ff;
        }

        String fileFormat = f.getAudioHeader().getFormat();

        Tag tag = f.getTag();

        if (tag != null) {
            List<TagField> ratingFieldsList = tag.getFields(FieldKey.RATING);

            // Sometimes this will contain more than one music rating tag (e.g. WMP RATING and POPULARIMETER)
            // We will treat the file as rated only when there is exactly one rating tag.
            if (ratingFieldsList.size() == 1) {
                for (TagField t : ratingFieldsList) {
                    byte[] rc = t.getRawContent();

                    ff.rating = getStarRatingFromByteArray(rc, fileFormat);
                }
            } else {
                ff.rating = -1;
            }

            ff.artist = tag.getFirst(FieldKey.ARTIST);

//            List<TagField> artistFieldsList = tag.getFields(FieldKey.ARTIST);
//            if (artistFieldsList.size() == 1) {
//                for (TagField t : artistFieldsList) {
//
//                    byte[] rc = t.getRawContent();
//                    ff.artist =  new String(rc, StandardCharsets.UTF_8);
//                }
//            }

            ff.title =  tag.getFirst(FieldKey.TITLE);

//            List<TagField> titleFieldsList = tag.getFields(FieldKey.TITLE);
//            if (titleFieldsList.size() == 1) {
//                for (TagField t : titleFieldsList) {
//                    byte[] rc = t.getRawContent();
//                    ff.title =  new String(rc, StandardCharsets.UTF_8);
//                }
//            }


        }

        return ff;

    }

    private static int getStarRatingFromByteArray(byte[] rc, String fo) {

        int rating = -1;

        String kmp3 = "MPEG-1 Layer 3";
        String kmp32 = "MPEG-1 Layer 2";
        String kmp33 = "MPEG-2 Layer 3";
        String ogg = "Ogg Vorbis v1";


        // Based on https://mutagen-specs.readthedocs.io/en/latest/id3/id3v2.3.0.html
        //
        // POPM tag consists of
        // 1. The email which is null terminated,
        // 2. after that it's 1 byte of rating and
        // 3. any number of bytes representing count (this part is optional,
        //    so it may not be there). So for up to ~255 plays it will be one byte,
        //    after that it takes 2 bytes, and after exceeding 2 bytes, can go to 3, etc.
        //
        // This means that need to get the first byte after last null terminator.

        byte b = 0;
        byte t;

        // Find the byte that is following the last null terminator
        for (int j = 0; j<rc.length;j++) {
            b = rc[rc.length - 1 - j];
            t = rc[rc.length - 2 - j];

            if (t == 0) break;
        }


//		byte b = rc[rc.length - 1];
        int i = b & 0xFF;

        if (kmp3.equals(fo) || kmp32.equals(fo) || kmp33.equals(fo)) {

            // Based on http://en.wikipedia.org/wiki/ID3#ID3v2_Rating_tag_issue

            if (i >=  1 && i <= 31)  rating = 1;
            else if (i >= 32 && i <= 95) rating = 2;
            else if (i >= 96 && i <= 159) rating = 3;
            else if (i >= 160 && i <= 223) rating = 4;
            else if (i >= 224 && i <= 255) rating = 5;

        } else if (ogg.equals(fo) ) {
            switch (rc[rc.length - 1]) {
                // ogg file ratings
                case 49: rating = 1; break;
                case 50: rating = 2; break;
                case 51: rating = 3; break;
                case 52: rating = 4; break;
                case 53: rating = 5; break;
                default: rating = -1;
            }
        }

        // A bit of debug information
//		if (rating == -1) {
//			System.out.println(fo);
//			System.out.println("bytevalue = " + i);
//		}


        return rating;
    }


}
