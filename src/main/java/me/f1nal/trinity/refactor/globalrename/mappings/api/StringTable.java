package me.f1nal.trinity.refactor.globalrename.mappings.api;

/**
 * @author itskekoff
 * @since 23:36 of 18.03.2025
 */
public enum StringTable {
    CHINA("的阿斯顿打赏和规范化士大夫文人喝几口接口了即可热舞热人风格的韩国法国海军固件库说客户法国人的发挥发和法国好几个太极图天地方个人各个地方给海关扣留哦韩国交给你吧v吧哪个好的风格和互动体验儿童也"),
    ALIEN("⏃⏚☊⎅⟒⎎☌⊑⟟⟊☍⌰⋔⋏⍜⌿⍾⍀⌇⏁⎍⎐⍙⌖⊬⋉"),
    EN("qwertyuiopasdfghjklzxcvbnm"),
    NUM("1234567890"),
    EMPTY("");

    private final char[] table;

    StringTable(String table) {
        this.table = (table.toLowerCase() + table.toUpperCase()).toCharArray();
    }

    public char getRandomChar() {
        return table[(int) (Math.random() * table.length)];
    }
}
