package cn.oa.knowledge.enums;

/**
 * 知识库常量.
 */
public final class KmConstants {

    private KmConstants() {
        // 工具类禁止实例化
    }

    /**
     * 知识条目状态.
     */
    public static final class EntryStatus {
        private EntryStatus() {}

        /** 草稿 */
        public static final String DRAFT = "DRAFT";
        /** 已发布 */
        public static final String PUBLISHED = "PUBLISHED";
        /** 已归档 */
        public static final String ARCHIVED = "ARCHIVED";
    }
}
