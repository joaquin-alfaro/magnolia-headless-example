package org.jokes.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class Joke {

    public String type;
    public Value value;

    public static class Value {

        public Integer id;
        public String joke;
        public List<String> categories = null;

    }
}

