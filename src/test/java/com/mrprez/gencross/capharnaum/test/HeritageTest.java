package com.mrprez.gencross.capharnaum.test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

import com.mrprez.gencross.Personnage;
import com.mrprez.gencross.Property;
import com.mrprez.gencross.disk.PersonnageFactory;
import com.mrprez.gencross.disk.PersonnageSaver;
import com.mrprez.gencross.value.Value;

public class HeritageTest {
	
	@Test
	public void test() throws Exception{
		PersonnageFactory personnageFactory = new PersonnageFactory(false);
		InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("Heritage.xml");
		Personnage personnage = personnageFactory.loadPersonnage(is);
		
		for(Property heritage : personnage.getProperty("Héritage").getSubProperties()){
			personnage.setNewValue(heritage, heritage.getOptions().get(heritage.getOptions().size() - 1).clone());
		}
		Personnage personnageRef = personnage.clone(); 
		
		for(Property heritage : personnage.getProperty("Héritage").getSubProperties()){
			for(Value option : heritage.getOptions()){
				boolean changeResult = personnage.setNewValue(heritage, option.clone());
				System.out.print(heritage.getName()+":"+option);
				System.out.println(changeResult?" SUCCESS":" FAILURE");
			}
		}
		
		personnageRef.getHistory().clear();
		personnage.getHistory().clear();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ByteArrayOutputStream baosRef = new ByteArrayOutputStream();
		PersonnageSaver.savePersonnage(personnage, baos);
		PersonnageSaver.savePersonnage(personnageRef, baosRef);
		String stringResult = new String(baos.toByteArray(), "UTF-8");
		String stringRef = new String(baosRef.toByteArray(), "UTF-8");
		Assert.assertEquals(stringRef, stringResult);
	}

}
