package ch.heigvd.amt.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity(name = "store")
public class Store {

  @Id
  @Column(name = "store_id")
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "manager_staff_id", referencedColumnName = "staff_id")
  private Staff managerStaff;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "address_id", referencedColumnName = "address_id")
  private Address address;

  public Store() {

  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Staff getManagerStaff() {
    return managerStaff;
  }

  public void setManagerStaff(Staff managerStaff) {
    this.managerStaff = managerStaff;
  }

  public Address getAddress() {
    return address;
  }

  public void setAddress(Address address) {
    this.address = address;
  }

  @Override
  public String toString() {
    return "Store{" +
            "id=" + id +
            ", managerStaff=" + managerStaff +
            ", address=" + address +
            '}';
  }
}