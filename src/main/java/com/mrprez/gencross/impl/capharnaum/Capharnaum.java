package com.mrprez.gencross.impl.capharnaum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import com.mrprez.gencross.Personnage;
import com.mrprez.gencross.Property;
import com.mrprez.gencross.history.ProportionalHistoryFactory;
import com.mrprez.gencross.value.IntValue;
import com.mrprez.gencross.value.StringValue;
import com.mrprez.gencross.value.Value;

public class Capharnaum extends Personnage {
	private static final String CARAC_CHOICE = "Choix caractéristique";
	private static final String COMPETENCE_CHOICE = "Choix compétence";
	

	@Override
	public void calculate(){
		super.calculate();
		if(phase.equals("Le sang et la parole")){
			calculateSangParole();
		}
		if(phase.equals("Figures")){
			calculateFiguresCompetences();
		}
		if(phase.equals("Finitions")){
			calculateFinitions();
		}
	}
	
	
	private void calculateFiguresCompetences() {
		List<Integer> profile = new LinkedList<Integer>(Arrays.asList(3,2,1,1,1,0,0,0));
		for(Property figure : getProperty("Compétences").getSubProperties()){
			int indexInProfile = profile.indexOf(figure.getValue().getInt());
			if(indexInProfile<0){
				errors.add("Vous devez respecter le profil dans la hiérarchie des figures: +3, +2, +1, +1, +1, 0, 0, 0");
				break;
			}
			profile.remove(indexInProfile);
		}
		
	}

	private void calculateSangParole(){
		if(getProperty("Sang").getValue().getString().isEmpty()){
			errors.add("Vous devez choisir votre Sang");
		} else if(getProperty("Tribu/Nation/Cité-Etat").getValue().getString().isEmpty()){
			errors.add("Vous devez choisir votre Tribu/Nation/Cité-Etat");
		}
		if(getProperty("Tribu/Nation/Cité-Etat#"+CARAC_CHOICE)!=null
				&& getProperty("Tribu/Nation/Cité-Etat#"+CARAC_CHOICE).getValue().getString().isEmpty()){
			errors.add("Vous devez choisir votre Caractéristique bonus");
		}
		if(getProperty("Parole").getValue().getString().isEmpty()){
			errors.add("Vous devez choisir votre Parole");
		}
		if(getProperty("Parole#"+COMPETENCE_CHOICE)!=null
				&& getProperty("Parole#"+COMPETENCE_CHOICE).getValue().getString().isEmpty()){
			errors.add("Vous devez choisir votre Compétence bonus");
		}
	}
	
	private void calculateFinitions(){
		for(Property heritage : getProperty("Héritage").getSubProperties()){
			if(heritage.getValue().getString().isEmpty()){
				errors.add("Vous devez lancer les dès pour votre "+heritage.getName());
			}
			if(heritage.getSubProperties()!=null){
				for(Property choice : heritage.getSubProperties()){
					if(choice.getValue().getString().isEmpty()){
						errors.add("Vous avez un choix à faire pour votre "+heritage.getName());
					}
				}
			}
		}
	}

	public void changeSang(Property sang, Value oldValue){
		String newSang = sang.getValue().getString();
		
		String clanOptions[] = appendix.getProperty("clans."+newSang).split("[,]");
		Property clan = getProperty("Tribu/Nation/Cité-Etat");
		clan.getOptions().clear();
		for(int i=0; i<clanOptions.length; i++){
			clan.getOptions().add(new StringValue(clanOptions[i]));
		}
		changeClan("", clan.getValue().getString());
		clan.setValue(new StringValue(""));
		clan.removeSubProperties();
	}
	
	public void changeClan(Property clan, Value oldValue){
		String oldClan = oldValue.getString();
		String newClan = clan.getValue().getString();
		changeClan(newClan, oldClan);
	}
	
	private void changeClan(String newClan, String oldClan){
		Property clan = getProperty("Tribu/Nation/Cité-Etat");
		// On enlève le bonus de l'ancien clan s'il y en avait déjà un d'établi ou de choisi.
		String oldCaracBonus = appendix.getProperty("bonus."+oldClan+".carac", "");
		if(oldCaracBonus.contains("|")){
			oldCaracBonus = clan.getSubProperty(CARAC_CHOICE).getValue().getString();
			clan.removeSubProperties();
		}
		if(!oldCaracBonus.isEmpty()){
			Property oldCarac = getProperty("Caractéristiques#"+oldCaracBonus);
			oldCarac.getValue().decrease();
		}
		
		// Si le nouveau clan à un bonus de caractéristique fixe, on l'ajoute directement, sinon on ajoute une property pour le choisir.
		String newCaracBonus = appendix.getProperty("bonus."+newClan+".carac", "");
		if(newCaracBonus.contains("|")){
			Property choiceCaracProperty = new Property(CARAC_CHOICE, new StringValue(""), clan);
			choiceCaracProperty.setOptions(newCaracBonus.split("[|]"));
			clan.addSubPropertiesList(true, false);
			clan.getSubProperties().add(choiceCaracProperty);
		}else if(!newCaracBonus.isEmpty()){
			Property newCarac = getProperty("Caractéristiques#"+newCaracBonus);
			newCarac.getValue().increase();
		}
		
		// On enlève les anciens bonus aux compétences
		if(!oldClan.isEmpty()){
			String oldCompetenceTab[] = appendix.getProperty("bonus."+oldClan+".competences").split(",");
			for(int i=0; i<oldCompetenceTab.length; i++){
				Property competence = getProperty("Compétences#"+oldCompetenceTab[i]);
				competence.getValue().decrease();
			}
		}
		
		// On ajoute les nouveaux bonus de compétence
		if(!newClan.isEmpty()){
			String newCompetenceTab[] = appendix.getProperty("bonus."+newClan+".competences").split(",");
			for(int i=0; i<newCompetenceTab.length; i++){
				Property competence = getProperty("Compétences#"+newCompetenceTab[i]);
				competence.getValue().increase();
			}
		}
	}
	
	public void changeParole(Property parole, Value oldValue){
		String newParole = parole.getValue().getString();
		String oldParole = oldValue.getString();
		// On enlève l'ancien bonus de caracteristique le cas échéant
		if(!oldParole.isEmpty()){
			String oldCarac = appendix.getProperty("bonus."+oldParole+".carac");
			getProperty("Caractéristiques#"+oldCarac).getValue().decrease();
		}
		
		// On ajoute le nouveau bonus de carac
		String newCarac = appendix.getProperty("bonus."+newParole+".carac");
		getProperty("Caractéristiques#"+newCarac).getValue().increase();
		
		// On enlève les anciens bonus aux compétences
		if(!oldParole.isEmpty()){
			String oldCompetenceTab[] = appendix.getProperty("bonus."+oldParole+".competences").split(",");
			for(int i=0; i<oldCompetenceTab.length; i++){
				if(oldCompetenceTab[i].contains("|")){
					String choice = parole.getSubProperty(COMPETENCE_CHOICE).getValue().getString();
					if(!choice.isEmpty()){
						getProperty("Compétences#"+choice).getValue().decrease();
					}
					parole.removeSubProperties();
				} else {
					getProperty("Compétences#"+oldCompetenceTab[i]).getValue().decrease();
				}
			}
		}
		
		// On ajoute les nouveaux bonus aux compétences
		String newCompetenceTab[] = appendix.getProperty("bonus."+newParole+".competences").split(",");
		for(int i=0; i<newCompetenceTab.length; i++){
			if(newCompetenceTab[i].contains("|")){
				Property choiceCompProperty = new Property(COMPETENCE_CHOICE, new StringValue(""), parole);
				choiceCompProperty.setOptions(newCompetenceTab[i].split("[|]"));
				parole.addSubPropertiesList(true, false);
				parole.getSubProperties().add(choiceCompProperty);
			} else {
				getProperty("Compétences#"+newCompetenceTab[i]).getValue().increase();
			}
		}
	}
	
	public void changeChoixCarac(Property caracChoice, Value oldValue){
		if(!oldValue.getString().isEmpty()){
			getProperty("Caractéristiques#"+oldValue.getString()).getValue().decrease();
		}
		getProperty("Caractéristiques#"+caracChoice.getValue().getString()).getValue().increase();
	}
	
	public void changeChoixCompetence(Property competenceChoice, Value oldValue){
		if(!oldValue.getString().isEmpty()){
			getProperty("Compétences#"+oldValue.getString()).getValue().decrease();
		}
		getProperty("Compétences#"+competenceChoice.getValue().getString()).getValue().increase();
	}
	
	public void changeFigure(Property figure, Value oldValue){
		int delta = figure.getValue().getInt()-oldValue.getInt();
		for(Property competence : figure.getSubProperties()){
			competence.setValue(new IntValue(competence.getValue().getInt() + delta));
		}
	}
	
	public void changeVerbeSacre(Property verbeSacre, Value oldValue){
		int value = verbeSacre.getValue().getInt();
		int newVerbesTotal;
		if(value >= 6){
			newVerbesTotal = 3;
		} else if(value >= 3){
			newVerbesTotal = 2;
		} else if(value >= 1){
			newVerbesTotal = 1;
		}else{
			newVerbesTotal = 0;
		}
		int oldVerbesTotal = pointPools.get("Verbes").getTotal();
		pointPools.get("Verbes").add(newVerbesTotal - oldVerbesTotal); 
		
		int oldElementTotal = pointPools.get("Elements magiques").getTotal();
		int newElementTotal = value * 2;
		pointPools.get("Elements magiques").add(newElementTotal - oldElementTotal);
	}
	
	
	public boolean checkHeritageSang(Property heritage, Value newValue){
		String sang = getProperty("Sang").getValue().getString();
		return checkHeritage(heritage, newValue, "heritage.sang."+sang+".");
	}
	
	public boolean checkHeritageFigure(Property heritage, Value newValue){
		return checkHeritage(heritage, newValue, "heritage.figure.");
	}
	
	private boolean checkHeritage(Property heritage, Value newValue, String appendixPrefix){
		String newHeritages[] = appendix.getProperty(appendixPrefix+newValue).split("[+]");
		
		for(int i=0; i<newHeritages.length; i++){
			String newHeritage = newHeritages[i];
			if(newHeritage.startsWith("competence.")){
				String competence = newHeritage.replace("competence.", "");
				if( ! competence.contains("|")){
					Property competenceProperty = getProperty("Compétences#"+competence);
					if(competenceProperty.getValue().getInt() >= 5){
						actionMessage = "Impossible de monter la compétence "+competenceProperty.getName()+" elle a déjà atteint son maximum";
						return false;
					}
				}
			}
			if(newHeritage.startsWith("carac.")){
				String caracName = newHeritage.replace("carac.", "");
				Property carac = getProperty("Caractéristiques").getSubProperty(caracName);
				if(carac.getValue().getInt() >= 4){
					actionMessage = "Impossible de monter la caractéristique "+caracName+" elle a déjà atteint son maximum";
					return false;
				}
			}
		}
		return true;
	}
	
	public boolean checkChoixCompetence(Property choiceProperty, Value newValue){
		Property competenceProperty = getProperty("Compétences#"+newValue.getString());
		if(competenceProperty.getValue().getInt() >= 5){
			actionMessage = "Impossible de monter la compétence "+competenceProperty.getName()+" elle a déjà atteint son maximum";
			return false;
		}
		return true;
	}
	
	public void changeHeritageSang(Property heritage, Value oldValue) throws Exception{
		String sang = getProperty("Sang").getValue().getString();
		changeHeritage(heritage, oldValue, "heritage.sang."+sang+".");
	}
	
	public void changeHeritageFigure(Property heritage, Value oldValue) throws Exception{
		changeHeritage(heritage, oldValue, "heritage.figure.");
	}
	
	public void changeHeritageDragon(Property heritage, Value oldValue) throws Exception{
		changeHeritage(heritage, oldValue, "heritage.dragon.");
	}
	
	public void changeHeritage(Property heritage, Value oldValue, String appendixPrefix) throws Exception{
		// On enlève l'ancien héritage
		if( ! oldValue.getString().isEmpty()){
			String oldHeritages[] = appendix.getProperty(appendixPrefix+oldValue).split("[+]");
			for(int i=0; i<oldHeritages.length; i++){
				String oldHeritage = oldHeritages[i];
				if(oldHeritage.startsWith("contact.")){
					int contact = Integer.parseInt(oldHeritage.replace("contact.", ""));
					pointPools.get("Contacts").add(-contact);
				}else if(oldHeritage.startsWith("vertu.")){
					String vertueName = oldHeritage.replace("vertu.", "");
					getProperty("Vertus Héroïques").getSubProperty(vertueName).getValue().decrease();
				}else if(oldHeritage.equals("parole")){
					Property choiceProperty = heritage.getSubProperty(COMPETENCE_CHOICE);
					if( ! choiceProperty.getValue().getString().isEmpty()){
						Property choosenProperty = getProperty("Compétences#"+heritage.getSubProperty(COMPETENCE_CHOICE).getValue());
						choosenProperty.getValue().decrease();
					}
					heritage.removeSubProperties();
				}else if(oldHeritage.startsWith("carac.")){
					String caracName = oldHeritage.replace("carac.", "");
					if(caracName.contains("|")){
						Property choiceProperty = heritage.getSubProperty(CARAC_CHOICE);
						if( ! choiceProperty.getValue().getString().isEmpty()){
							Property carac = getProperty("Caractéristiques").getSubProperty(choiceProperty.getValue().getString());
							carac.getValue().decrease();
						}
						heritage.removeSubProperties();
					}else{
						Property carac = getProperty("Caractéristiques").getSubProperty(caracName);
						carac.getValue().decrease();
					}
				}else if(oldHeritage.equals("competencesPrincipale")){
					Property choiceProperty1 = heritage.getSubProperty(COMPETENCE_CHOICE+" 1");
					Property choiceProperty2 = heritage.getSubProperty(COMPETENCE_CHOICE+" 2");
					if( ! choiceProperty1.getValue().getString().isEmpty() ){
						Property choosenProperty = getProperty("Compétences#"+choiceProperty1.getValue());
						choosenProperty.getValue().decrease();
					}
					if( ! choiceProperty2.getValue().getString().isEmpty() ){
						Property choosenProperty = getProperty("Compétences#"+choiceProperty2.getValue());
						choosenProperty.getValue().decrease();
					}
					heritage.removeSubProperties();
				}else if(oldHeritage.startsWith("competence.")){
					String competence = oldHeritage.replace("competence.", "");
					if(competence.contains("|")){
						Property choiceProperty = heritage.getSubProperty(COMPETENCE_CHOICE);
						if( ! choiceProperty.getValue().getString().isEmpty()){
							Property choosenProperty = getProperty("Compétences#"+heritage.getSubProperty(COMPETENCE_CHOICE).getValue());
							choosenProperty.getValue().decrease();
						}
						heritage.removeSubProperties();
					}else{
						getProperty("Compétences#"+competence).getValue().decrease();
					}
				}else if(oldHeritage.startsWith("possession.")){
					String name = oldHeritage.replace("possession.", "");
					getProperty("Possessions").getSubProperties().remove(name);
				}else if(oldHeritage.startsWith("richesse.")){
					int bonusRichesse = Integer.parseInt(oldHeritage.replace("richesse.", ""));
					Property richesse = getProperty("Possessions").getSubProperty("Niveau de richesse");
					richesse.setValue(new IntValue(richesse.getValue().getInt() - bonusRichesse));
				}else if(oldHeritage.equals("special")){
					;
				}else{
					throw new Exception("Incorrect appendix property: "+oldHeritage);
				}
			}
		}
		
		// On ajoute le nouveau
		if( ! heritage.getValue().getString().isEmpty()){
			String newHeritages[] = appendix.getProperty(appendixPrefix+heritage.getValue()).split("[+]");
			for(int i=0; i<newHeritages.length; i++){
				String newHeritage = newHeritages[i];
				if(newHeritage.startsWith("contact.")){
					int contact = Integer.parseInt(newHeritage.replace("contact.", ""));
					pointPools.get("Contacts").add(contact);
				}else if(newHeritage.startsWith("vertu.")){
					String vertueName = newHeritage.replace("vertu.", "");
					getProperty("Vertus Héroïques").getSubProperty(vertueName).getValue().increase();
				}else if(newHeritage.equals("parole")){
					String parole = getProperty("Parole").getValue().getString();
					Property choiceProperty = new Property(COMPETENCE_CHOICE, heritage);
					choiceProperty.setValue(new StringValue(""));
					choiceProperty.setOptions(appendix.getProperty("bonus."+parole+".competences").split("[,]"));
					heritage.addSubPropertiesList(true, false).add(choiceProperty);
				}else if(newHeritage.startsWith("carac.")){
					String caracName = newHeritage.replace("carac.", "");
					if(caracName.contains("|")){
						Property choiceProperty = new Property(CARAC_CHOICE, heritage);
						choiceProperty.setValue(new StringValue(""));
						choiceProperty.setOptions(caracName.split("[|]"));
						heritage.addSubPropertiesList(true, false).add(choiceProperty);
					}else{
						Property carac = getProperty("Caractéristiques").getSubProperty(caracName);
						carac.getValue().increase();
					}
				}else if(newHeritage.equals("competencesPrincipale")){
					String principale = getFigurePrincipale();
					Property choiceProperty1 = new Property(COMPETENCE_CHOICE+" 1", heritage);
					Property choiceProperty2 = new Property(COMPETENCE_CHOICE+" 2", heritage);
					choiceProperty1.setValue(new StringValue(""));
					choiceProperty2.setValue(new StringValue(""));
					choiceProperty1.setOptions(new ArrayList<Value>(4));
					choiceProperty2.setOptions(new ArrayList<Value>(4));
					for(Property competence : getProperty("Compétences#").getSubProperty(principale).getSubProperties()){
						choiceProperty1.getOptions().add(new StringValue(principale+"#"+competence.getName()));
						choiceProperty2.getOptions().add(new StringValue(principale+"#"+competence.getName()));
					}
					heritage.addSubPropertiesList(true, false);
					heritage.getSubProperties().add(choiceProperty1);
					heritage.getSubProperties().add(choiceProperty2);
				}else if(newHeritage.startsWith("competence.")){
					String competence = newHeritage.replace("competence.", "");
					if(competence.contains("|")){
						Property choiceProperty = new Property(COMPETENCE_CHOICE, heritage);
						choiceProperty.setValue(new StringValue(""));
						choiceProperty.setOptions(competence.split("[|]"));
						heritage.addSubPropertiesList(true, false).add(choiceProperty);
					}else{
						getProperty("Compétences#"+competence).getValue().increase();
					}
				}else if(newHeritage.startsWith("possession.")){
					String name = newHeritage.replace("possession.", "");
					Property possessions = getProperty("Possessions");
					Property possession = new Property(name, new StringValue(""), possessions);
					possession.setRemovable(false);
					getProperty("Possessions").getSubProperties().add(possession);
				}else if(newHeritage.startsWith("richesse.")){
					int bonusRichesse = Integer.parseInt(newHeritage.replace("richesse.", ""));
					Property richesse = getProperty("Possessions").getSubProperty("Niveau de richesse");
					richesse.setValue(new IntValue(richesse.getValue().getInt() + bonusRichesse));
				}else if(newHeritage.equals("special")){
					;
				}else{
					throw new Exception("Incorrect appendix property: "+newHeritage);
				}
			}
			
		}
	}
	
	public void endSangParole(){
		if(getProperty("Tribu/Nation/Cité-Etat#"+CARAC_CHOICE)!=null){
			getProperty("Tribu/Nation/Cité-Etat").removeSubProperties();
		}
		if(getProperty("Parole#"+COMPETENCE_CHOICE)!=null){
			getProperty("Parole").removeSubProperties();
		}
	}
	
	public void passToCompetences(){
		changeVerbeSacre(getProperty("Compétences#Le Sorcier#Verbe sacré"), getProperty("Compétences#Le Sorcier#Verbe sacré").getValue());
		
		for(Property figure : getProperty("Compétences").getSubProperties()){
			figure.setOptions((List<Value>) null);
			figure.setEditable(false);
			for(Property competence : figure.getSubProperties()){
				competence.setMin();
				int max = Math.min(5, competence.getValue().getInt() + 2);
				competence.setMax(new IntValue(max));
				competence.setEditable(true);
				competence.setHistoryFactory(new ProportionalHistoryFactory("Compétences"));
			}
		}
	}
	
	public void passToFinition(){
		getProperty("Compétences").setEditableRecursivly(false);
		
		String sang = getProperty("Sang").getValue().getString();
		Property heritageSang1 = getProperty("Héritage#Sang 1");
		Property heritageSang2 = getProperty("Héritage#Sang 2");
		SortedSet<Integer> indexes = new TreeSet<Integer>();
		for(String key : appendix.getSubMap("heritage.sang."+sang+".").keySet()){
			indexes.add(new Integer(key.replace("heritage.sang."+sang+".", "")));
		}
		for(Integer index : indexes){
			heritageSang1.getOptions().add(new StringValue(index.toString()));
			heritageSang2.getOptions().add(new StringValue(index.toString()));
		}
		
		String figurePrincipale = getFigurePrincipale();
		Property heritageFigurePrincipale = getProperty("Héritage#Figure principale");
		for(int i=2; i<=12; i++){
			heritageFigurePrincipale.getOptions().add(new StringValue(figurePrincipale+"."+i));
		}
		
		Property heritageFigure1 = getProperty("Héritage#Figure 1");
		Property heritageFigure2 = getProperty("Héritage#Figure 2");
		for(Property figure : getProperty("Compétences").getSubProperties()){
			for(int i=2; i<=12; i++){
				heritageFigure1.getOptions().add(new StringValue(figure.getName()+"."+i));
				heritageFigure2.getOptions().add(new StringValue(figure.getName()+"."+i));
			}
		}
		
		Property richesse = getProperty("Possessions").getSubProperty("Niveau de richesse");
		Property prince = getProperty("Compétences").getSubProperty("Le Prince");
		richesse.setValue(prince.getValue().clone());
		
	}
	

	private String getFigurePrincipale(){
		for(Property figure : getProperty("Compétences").getSubProperties()){
			if(figure.getValue().getInt() == 3){
				return figure.getName();
			}
		}
		return null;
	}
}
