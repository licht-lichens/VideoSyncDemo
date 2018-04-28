package com.lichens.licht.filedownload;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

/**
 * Created by licht on 2018/4/25.
 */

@Entity
public class User {
    @Id
    private Long id;
    private Integer thread_id;
    private Integer start_pos;
    private Integer end_pos;
    private Integer compelete_size;
    private String url;
    @Generated(hash = 2041931179)
    public User(Long id, Integer thread_id, Integer start_pos, Integer end_pos,
                Integer compelete_size, String url) {
        this.id = id;
        this.thread_id = thread_id;
        this.start_pos = start_pos;
        this.end_pos = end_pos;
        this.compelete_size = compelete_size;
        this.url = url;
    }
    @Generated(hash = 586692638)
    public User() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Integer getThread_id() {
        return this.thread_id;
    }
    public void setThread_id(Integer thread_id) {
        this.thread_id = thread_id;
    }
    public Integer getStart_pos() {
        return this.start_pos;
    }
    public void setStart_pos(Integer start_pos) {
        this.start_pos = start_pos;
    }
    public Integer getEnd_pos() {
        return this.end_pos;
    }
    public void setEnd_pos(Integer end_pos) {
        this.end_pos = end_pos;
    }
    public Integer getCompelete_size() {
        return this.compelete_size;
    }
    public void setCompelete_size(Integer compelete_size) {
        this.compelete_size = compelete_size;
    }
    public String getUrl() {
        return this.url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
}
