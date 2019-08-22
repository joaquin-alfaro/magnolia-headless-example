package news.org.ws.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Calendar;

@Data
@NoArgsConstructor
public class NewsDto {

    private String identifier;
    private String author;
    private String name;
    private Calendar publicationDate;
    private String title;
    private String summary;
    private String image;
    private String detail;
    private String authorJoke;

}
