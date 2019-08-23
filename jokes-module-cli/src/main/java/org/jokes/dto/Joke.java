package org.jokes.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
public class Joke implements Serializable {

    public String type;
    public Value value;

    public static class Value implements Serializable {

        public Integer id;
        public String joke;
        public List<String> categories = null;

    }
}

