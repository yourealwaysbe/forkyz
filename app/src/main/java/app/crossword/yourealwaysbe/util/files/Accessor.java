package app.crossword.yourealwaysbe.util.files;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;


public interface Accessor extends Comparator<PuzMetaFile> {
    Accessor DATE_ASC = new Accessor() {
            public String getLabel(PuzMetaFile o) {
                DateTimeFormatter df
                    = DateTimeFormatter.ofPattern("EEEE MMM dd, yyyy");

                return df.format(o.getDate());
            }

            public int compare(PuzMetaFile object1, PuzMetaFile object2) {
                return object1.getDate()
                              .compareTo(object2.getDate());
            }
        };

    Accessor DATE_DESC = new Accessor() {
            public String getLabel(PuzMetaFile o) {
                DateTimeFormatter df
                    = DateTimeFormatter.ofPattern("EEEE MMM dd, yyyy");

                return df.format(o.getDate());
            }

            public int compare(PuzMetaFile object1, PuzMetaFile object2) {
                return object2.getDate()
                              .compareTo(object1.getDate());
            }
        };

    Accessor SOURCE = new Accessor() {
            public String getLabel(PuzMetaFile o) {
                return o.getSource();
            }

            public int compare(PuzMetaFile object1, PuzMetaFile object2) {
                return object1.getSource()
                              .compareTo(object2.getSource());
            }
        };

    String getLabel(PuzMetaFile o);
}
