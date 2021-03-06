/*
 * Aipo is a groupware program developed by TOWN, Inc.
 * Copyright (C) 2004-2015 TOWN, Inc.
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
package com.aipo.orm.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.mail.internet.MimeUtility;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.access.DataContext;

import com.aipo.orm.Database;
import com.aipo.orm.model.security.TurbineUser;
import com.aipo.orm.query.Operations;
import com.aipo.orm.query.SQLTemplate;
import com.aipo.orm.service.request.SearchOptions;
import com.aipo.orm.service.request.SearchOptions.FilterOperation;
import com.aipo.orm.service.request.SearchOptions.SortOrder;
import com.google.inject.Singleton;

@Singleton
public class AipoTurbineUserDbService implements TurbineUserDbService {

  public static int MAX_LIMIT = 1000;

  @Override
  public int getCountByGroupname(String groupname, SearchOptions options) {
    List<DataRow> dataRows =
      queryByGroupname(groupname, options, true).fetchListAsDataRow();
    if (dataRows.size() == 1) {
      DataRow dataRow = dataRows.get(0);
      Object count = dataRow.get("count");
      if (count == null) {
        count = dataRow.get("COUNT(*)");
      }
      return ((Long) count).intValue();
    }
    return 0;
  }

  @Override
  public List<TurbineUser> findByGroupname(String groupname,
      SearchOptions options) {
    SQLTemplate<TurbineUser> selectBySql =
      queryByGroupname(groupname, options, false);
    if (selectBySql == null) {
      return new ArrayList<TurbineUser>();
    }
    return selectBySql.fetchList();
  }

  @Override
  public TurbineUser findByUsername(String username) {
    if (username == null) {
      return null;
    }

    StringBuilder b = new StringBuilder();
    b
      .append(" SELECT B.USER_ID, B.LOGIN_NAME, B.FIRST_NAME, B.LAST_NAME, B.FIRST_NAME_KANA, B.LAST_NAME_KANA, B.PHOTO_MODIFIED, B.PASSWORD_VALUE, B.HAS_PHOTO ");
    b.append(" FROM turbine_user AS B ");
    b.append(" WHERE B.USER_ID > 3 AND B.DISABLED = 'F' ");
    b.append(" AND B.LOGIN_NAME = #bind($username) ");

    String query = b.toString();

    return Database
      .sql(TurbineUser.class, query)
      .param("username", username)
      .fetchSingle();
  }

  @Override
  public List<TurbineUser> findByUsername(Set<String> username) {
    if (username == null || username.size() == 0) {
      return null;
    }

    StringBuilder b = new StringBuilder();
    b
      .append(" SELECT B.USER_ID, B.LOGIN_NAME, B.FIRST_NAME, B.LAST_NAME, B.PHOTO_MODIFIED, B.HAS_PHOTO");
    b.append(" FROM turbine_user AS B ");
    b.append(" WHERE B.USER_ID > 3 AND B.DISABLED = 'F' ");
    b.append(" AND B.LOGIN_NAME IN(#bind($username)) ");

    String query = b.toString();

    return Database
      .sql(TurbineUser.class, query)
      .param("username", username)
      .fetchList();
  }

  @Override
  public List<TurbineUser> find(SearchOptions options) {
    return buildQuery(
      " B.USER_ID, B.LOGIN_NAME, B.FIRST_NAME, B.LAST_NAME, B.FIRST_NAME_KANA, B.LAST_NAME_KANA, B.PHOTO_MODIFIED, D.POSITION, B.HAS_PHOTO ",
      options,
      false).fetchList();
  }

  @Override
  public int getCount(SearchOptions options) {
    List<DataRow> dataRows =
      buildQuery(" COUNT(*) ", options, true).fetchListAsDataRow();
    if (dataRows.size() == 1) {
      DataRow dataRow = dataRows.get(0);
      Object count = dataRow.get("count");
      if (count == null) {
        count = dataRow.get("COUNT(*)");
      }
      return ((Long) count).intValue();
    }
    return 0;
  }

  protected SQLTemplate<TurbineUser> buildQuery(String selectColumns,
      SearchOptions options, boolean isCount) {

    int limit = options.getLimit();
    int offset = options.getOffset();
    if (limit > MAX_LIMIT) {
      limit = MAX_LIMIT;
    }

    StringBuilder b = new StringBuilder();
    b.append(" SELECT ");
    b.append(selectColumns);
    b.append(" FROM turbine_user AS B ");
    b.append(" LEFT JOIN eip_m_user_position AS D ");
    b.append(" ON B.USER_ID = D.USER_ID ");
    b.append(" WHERE B.USER_ID > 3 AND B.DISABLED = 'F' ");

    // Filter
    String filter = options.getFilterBy();
    FilterOperation filterOperation = options.getFilterOperation();
    String filterValue = options.getFilterValue();
    boolean isFilter = false;
    String paramKey = "filter";
    Object paramValue = null;
    // 氏名
    if ("name".equals(filter)) {
      switch (filterOperation) {
        case equals:
          if (Database.isJdbcPostgreSQL()) {
            b.append(" AND B.LAST_NAME || B.FIRST_NAME = #bind($filter) ");
          } else {
            b.append(" AND CONCAT(B.LAST_NAME,B.FIRST_NAME) = #bind($filter) ");
          }
          paramValue = filterValue;
          isFilter = true;
          break;
        case contains:
          if (Database.isJdbcPostgreSQL()) {
            b.append(" AND B.LAST_NAME || B.FIRST_NAME like #bind($filter) ");
          } else {
            b
              .append(" AND CONCAT(B.LAST_NAME,B.FIRST_NAME) like #bind($filter) ");
          }
          paramValue = "%" + filterValue + "%";
          isFilter = true;
          break;
        case present:
          // not supported.
          break;
        case startsWith:
          if (Database.isJdbcPostgreSQL()) {
            b.append(" AND B.LAST_NAME || B.FIRST_NAME like #bind($filter) ");
          } else {
            b
              .append(" AND CONCAT(B.LAST_NAME,B.FIRST_NAME) like #bind($filter) ");
          }
          paramValue = filterValue + "%";
          isFilter = true;
          break;
        default:
          break;
      }
    }

    if (!isCount) {
      // Sort
      boolean isOrder = false;
      String sort = options.getSortBy();
      SortOrder sortOrder = options.getSortOrder();
      if ("position".equals(sort)) {
        if (SortOrder.ascending.equals(sortOrder)) {
          b.append(" ORDER BY D.POSITION ");
        } else {
          b.append(" ORDER BY D.POSITION DESC ");
        }
        isOrder = true;
      }
      if (!isOrder) {
        b.append(" ORDER BY D.POSITION ");
      }

      if (limit > 0) {
        b.append(" LIMIT ");
        b.append(limit);
      }

      if (offset > 0) {
        b.append(" OFFSET ");
        b.append(offset);
      }
    }

    String query = b.toString();

    SQLTemplate<TurbineUser> sqlTemplate =
      Database.sql(TurbineUser.class, query);
    if (isFilter) {
      sqlTemplate.param(paramKey, paramValue);
    }
    return sqlTemplate;
  }

  protected SQLTemplate<TurbineUser> queryByGroupname(String groupname,
      SearchOptions options, boolean isCount) {
    if (groupname == null) {
      return null;
    }

    int limit = options.getLimit();
    int offset = options.getOffset();
    if (limit > MAX_LIMIT) {
      limit = MAX_LIMIT;
    }

    StringBuilder b = new StringBuilder();
    if (isCount) {
      b.append(" SELECT COUNT(*) ");
    } else {
      b
        .append(" SELECT B.USER_ID, B.LOGIN_NAME, B.FIRST_NAME, B.LAST_NAME, B.FIRST_NAME_KANA, B.LAST_NAME_KANA, B.PHOTO_MODIFIED, D.POSITION, B.HAS_PHOTO ");
    }
    b.append(" FROM turbine_user_group_role AS A ");
    b.append(" LEFT JOIN turbine_user AS B ");
    b.append(" ON A.USER_ID = B.USER_ID ");
    b.append(" LEFT JOIN turbine_group AS C ");
    b.append(" ON A.GROUP_ID = C.GROUP_ID ");
    b.append(" LEFT JOIN eip_m_user_position AS D ");
    b.append(" ON A.USER_ID = D.USER_ID ");
    b.append(" WHERE B.USER_ID > 3 AND B.DISABLED = 'F' ");
    b.append(" AND C.GROUP_NAME = #bind($groupname) ");

    // Filter
    String filter = options.getFilterBy();
    FilterOperation filterOperation = options.getFilterOperation();
    String filterValue = options.getFilterValue();
    boolean isFilter = false;
    String paramKey = "filter";
    Object paramValue = null;
    // 氏名
    if ("name".equals(filter)) {
      switch (filterOperation) {
        case equals:
          if (Database.isJdbcPostgreSQL()) {
            b.append(" AND B.LAST_NAME || B.FIRST_NAME = #bind($filter) ");
          } else {
            b.append(" AND CONCAT(B.LAST_NAME,B.FIRST_NAME) = #bind($filter) ");
          }
          paramValue = filterValue;
          isFilter = true;
          break;
        case contains:
          if (Database.isJdbcPostgreSQL()) {
            b.append(" AND B.LAST_NAME || B.FIRST_NAME like #bind($filter) ");
          } else {
            b
              .append(" AND CONCAT(B.LAST_NAME,B.FIRST_NAME) like #bind($filter) ");
          }
          paramValue = "%" + filterValue + "%";
          isFilter = true;
          break;
        case present:
          // not supported.
          break;
        case startsWith:
          if (Database.isJdbcPostgreSQL()) {
            b.append(" AND B.LAST_NAME || B.FIRST_NAME like #bind($filter) ");
          } else {
            b
              .append(" AND CONCAT(B.LAST_NAME,B.FIRST_NAME) like #bind($filter) ");
          }
          paramValue = filterValue + "%";
          isFilter = true;
          break;
        default:
          break;
      }
    }

    if (!isCount) {
      // Sort
      boolean isOrder = false;
      String sort = options.getSortBy();
      SortOrder sortOrder = options.getSortOrder();
      if ("position".equals(sort)) {
        if (SortOrder.ascending.equals(sortOrder)) {
          b.append(" ORDER BY D.POSITION ");
        } else {
          b.append(" ORDER BY D.POSITION DESC ");
        }
        isOrder = true;
      }
      if (!isOrder) {
        b.append(" ORDER BY D.POSITION ");
      }

      if (limit > 0) {
        b.append(" LIMIT ");
        b.append(limit);
      }

      if (offset > 0) {
        b.append(" OFFSET ");
        b.append(offset);
      }
    }

    String query = b.toString();

    SQLTemplate<TurbineUser> sqlTemplate =
      Database.sql(TurbineUser.class, query).param("groupname", groupname);
    if (isFilter) {
      sqlTemplate.param(paramKey, paramValue);
    }

    return sqlTemplate;
  }

  /**
   * @param username
   * @param password
   * @return
   */
  @Override
  public TurbineUser auth(String username, String password) {
    return auth("org001", username, password);
  }

  protected TurbineUser auth(String orgId, String username, String password) {

    DataContext dataContext = null;
    try {
      dataContext = Database.createDataContext(orgId);
      DataContext.bindThreadObjectContext(dataContext);
    } catch (Throwable t) {
      t.printStackTrace();
    }

    TurbineUser user = findByUsername(username);
    if (user == null) {
      return null;
    }
    String encodedPassword = null;
    try {
      MessageDigest md = MessageDigest.getInstance("SHA");
      // We need to use unicode here, to be independent of platform's
      // default encoding. Thanks to SGawin for spotting this.
      byte[] digest = md.digest(password.getBytes("UTF-8"));
      ByteArrayOutputStream bas =
        new ByteArrayOutputStream(digest.length + digest.length / 3 + 1);
      OutputStream encodedStream = MimeUtility.encode(bas, "base64");
      encodedStream.write(digest);
      encodedStream.flush();
      encodedStream.close();
      encodedPassword = bas.toString();
    } catch (Throwable ignore) {
      // ignore
    }
    if (encodedPassword == null) {
      return null;
    }
    if (encodedPassword.equals(user.getPasswordValue())) {
      return user;
    } else {
      return null;
    }
  }

  /**
   * @param username
   * @return
   */
  @Override
  public InputStream getPhoto(String username) {
    if (username == null) {
      return null;
    }

    StringBuilder b = new StringBuilder();
    b
      .append(" SELECT B.USER_ID, B.LOGIN_NAME, B.FIRST_NAME, B.LAST_NAME, B.FIRST_NAME_KANA, B.LAST_NAME_KANA, B.PASSWORD_VALUE, B.PHOTO ");
    b.append(" FROM turbine_user AS B ");
    b.append(" WHERE B.USER_ID > 3 AND B.DISABLED = 'F' ");
    b.append(" AND B.LOGIN_NAME = #bind($username) ");

    String query = b.toString();

    TurbineUser tuser =
      Database
        .sql(TurbineUser.class, query)
        .param("username", username)
        .fetchSingle();

    if (tuser == null) {
      return null;
    }

    byte[] photo = tuser.getPhoto();
    if (photo == null) {
      return null;
    }

    return new ByteArrayInputStream(photo);
  }

  /**
   * @param userId
   * @param profileIcon
   * @param profileIconSmartPhone
   */
  @Override
  public void setPhoto(String username, byte[] profileIcon,
      byte[] profileIconSmartPhone) {
    try {
      TurbineUser user =
        Database
          .query(TurbineUser.class)
          .where(Operations.eq(TurbineUser.LOGIN_NAME_PROPERTY, username))
          .fetchSingle();

      if (user == null) {
        return;
      }

      if (profileIcon != null && profileIconSmartPhone != null) {
        // 顔写真を登録する．
        user.setPhotoSmartphone(profileIconSmartPhone);
        user.setHasPhotoSmartphone("N");
        user.setPhotoModifiedSmartphone(new Date());
        user.setPhoto(profileIcon);
        user.setHasPhoto("N");
        user.setPhotoModified(new Date());
      } else {
        user.setPhotoSmartphone(null);
        user.setHasPhotoSmartphone("F");
        user.setPhotoModifiedSmartphone(new Date());
        user.setPhoto(null);
        user.setHasPhoto("F");
        user.setPhotoModified(new Date());
      }
      Database.commit();
    } catch (Throwable t) {
      Database.rollback();
      throw new RuntimeException(t);
    }

  }
}
