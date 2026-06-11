package com.radolyn.ayugram.localbots;

import java.util.ArrayList;

public class LocalBot {
    public String id;
    public String name;
    public String purpose;
    public long dialogId;
    public boolean enabled = true;
    public ArrayList<Block> blocks = new ArrayList<>();

    public static class Block {
        public String blockId;
        public String nextBlockId;
        public String type;
        public String value;
        public String param1;
        public String param2;
        public String param3;
        public int x;
        public int y;

        public Block() {
        }

        public Block(String type, String value) {
            this.type = type;
            this.value = value;
        }

        public Block(String blockId, String type, String value, int x, int y) {
            this.blockId = blockId;
            this.type = type;
            this.value = value;
            this.x = x;
            this.y = y;
        }
    }
}
