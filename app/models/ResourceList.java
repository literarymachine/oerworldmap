package models;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author fo
 */
public class ResourceList {

  private List<Resource> items;

  private long totalItems;

  private long itemsPerPage;

  private String sortOrder;

  private String searchTerms;

  private int offset;

  public ResourceList(@Nonnull List<Resource> aResourceList, long aTotalItems, String aSearchTerms, int aOffset, int aSize, String aSortOrder) {
    items = aResourceList;
    totalItems = aTotalItems;
    searchTerms = aSearchTerms;
    offset = aOffset;
    itemsPerPage = aSize;
    sortOrder = aSortOrder;
  }

  public List<Resource> getItems() {
    return this.items;
  }

  // TODO: remove setter when unwrapping records in BaseRepository becomes unnecessary
  public void setItems(List<Resource> items) {
    this.items = items;
  }

  public long getTotalItems() {
    return this.totalItems;
  }

  public long getItemsPerPage() {
    return this.itemsPerPage;
  }

  public String getNextPage() {

    if (offset + itemsPerPage >= totalItems) {
      return null;
    }

    ArrayList<String> params = new ArrayList<>();
    if (!StringUtils.isEmpty(searchTerms)) {
      params.add("q=".concat(searchTerms));
    }
    params.add("from=".concat(Long.toString(offset + itemsPerPage)));
    params.add("size=".concat(Long.toString(itemsPerPage)));
    if (!StringUtils.isEmpty(sortOrder)) {
      params.add("sort=".concat(sortOrder));
    }
    return params.isEmpty() ? null : "?".concat(StringUtils.join(params, "&"));
  }


  public String getPreviousPage() {

    if (offset - itemsPerPage < 0) {
      return null;
    }

    ArrayList<String> params = new ArrayList<>();
    if (!StringUtils.isEmpty(searchTerms)) {
      params.add("q=".concat(searchTerms));
    }
    params.add("from=".concat(Long.toString(offset - itemsPerPage)));
    params.add("size=".concat(Long.toString(itemsPerPage)));
    if (!StringUtils.isEmpty(sortOrder)) {
      params.add("sort=".concat(sortOrder));
    }
    return params.isEmpty() ? null : "?".concat(StringUtils.join(params, "&"));
  }

  public String getFirstPage() {

    if (offset <= 0) {
      return null;
    }

    ArrayList<String> params = new ArrayList<>();
    if (!StringUtils.isEmpty(searchTerms)) {
      params.add("q=".concat(searchTerms));
    }
    params.add("from=0");
    params.add("size=".concat(Long.toString(itemsPerPage)));
    if (!StringUtils.isEmpty(sortOrder)) {
      params.add("sort=".concat(sortOrder));
    }
    return params.isEmpty() ? null : "?".concat(StringUtils.join(params, "&"));
  }

  public String getLastPage() {

    if (offset + itemsPerPage >= totalItems) {
      return null;
    }

    ArrayList<String> params = new ArrayList<>();
    if (!StringUtils.isEmpty(searchTerms)) {
      params.add("q=".concat(searchTerms));
    }
    if ((totalItems / itemsPerPage) * itemsPerPage == totalItems) {
      params.add("from=".concat(Long.toString((totalItems / itemsPerPage) * itemsPerPage - itemsPerPage)));
    } else {
      params.add("from=".concat(Long.toString((totalItems / itemsPerPage) * itemsPerPage)));
    }
    params.add("size=".concat(Long.toString(itemsPerPage)));
    if (!StringUtils.isEmpty(sortOrder)) {
      params.add("sort=".concat(sortOrder));
    }
    return params.isEmpty() ? null : "?".concat(StringUtils.join(params, "&"));
  }

  public String getSortOrder() {
    return this.sortOrder;
  }

  // TODO: remove setter when filter appended to search terms becomes unnecessary
  public void setSearchTerms(String searchTerms) {
    this.searchTerms = searchTerms;
  }

  public String getSearchTerms() {
    return this.searchTerms;
  }

  public Resource toResource() {
    Resource pagedCollection = new Resource("PagedCollection");
    pagedCollection.put("totalItems", totalItems);
    pagedCollection.put("itemsPerPage", itemsPerPage);
    pagedCollection.put("nextPage", getNextPage());
    pagedCollection.put("previousPage", getPreviousPage());
    pagedCollection.put("lastPage", getLastPage());
    pagedCollection.put("firstPage", getFirstPage());
    pagedCollection.put("member", items);
    return pagedCollection;
  }

}
