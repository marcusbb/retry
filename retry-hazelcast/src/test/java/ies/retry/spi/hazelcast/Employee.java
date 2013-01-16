package ies.retry.spi.hazelcast;

import java.io.Serializable;

public class Employee implements Serializable {
	
	private static final long serialVersionUID = -790544225333865972L;

	private String name;
	private int age;
	private String title;
	public Employee() {}
	public Employee(String name,int age, String title) {
		this.name = name;
		this.age = age;
		this.title = title;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getAge() {
		return age;
	}
	public void setAge(int age) {
		this.age = age;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	@Override
	public String toString() {
		return "Employee [name=" + name + ", age=" + age + ", title=" + title
				+ "]";
	}
	
	@Override
	public int hashCode() { //added
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj) return true;
		if((obj == null) || (obj.getClass() != this.getClass())) return false;
		return ((Employee)obj).name.equals(name);
	}

	
	
}
