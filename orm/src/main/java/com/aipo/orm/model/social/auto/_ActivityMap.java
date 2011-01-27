/*
 * Aipo is a groupware program developed by Aimluck,Inc.
 * Copyright (C) 2004-2011 Aimluck,Inc.
 * http://www.aipo.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.aipo.orm.model.social.auto;

import org.apache.cayenne.CayenneDataObject;

import com.aipo.orm.model.social.Activity;

/**
 * Class _ActivityMap was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _ActivityMap extends CayenneDataObject {

    public static final String IS_READ_PROPERTY = "isRead";
    public static final String LOGIN_NAME_PROPERTY = "loginName";
    public static final String ACTIVITY_PROPERTY = "activity";

    public static final String ID_PK_COLUMN = "ID";

    public void setIsRead(Short isRead) {
        writeProperty("isRead", isRead);
    }
    public Short getIsRead() {
        return (Short)readProperty("isRead");
    }

    public void setLoginName(String loginName) {
        writeProperty("loginName", loginName);
    }
    public String getLoginName() {
        return (String)readProperty("loginName");
    }

    public void setActivity(Activity activity) {
        setToOneTarget("activity", activity, true);
    }

    public Activity getActivity() {
        return (Activity)readProperty("activity");
    }


}
