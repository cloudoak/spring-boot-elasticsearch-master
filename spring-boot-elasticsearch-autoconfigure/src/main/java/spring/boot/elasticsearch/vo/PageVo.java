package spring.boot.elasticsearch.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Elasticseacrh index value object.
 *
 * @author OAK
 * @since 2019/06/25 14:29:00 PM.
 * @version 1.0
 */
@Data
@AllArgsConstructor
public class PageVo extends BaseVo {

    /**
     * from size.
     */
    private Integer from;

    /**
     * total rows size.
     */
    private Integer size;

}
