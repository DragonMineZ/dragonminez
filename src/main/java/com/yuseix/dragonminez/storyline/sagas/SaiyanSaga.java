package com.yuseix.dragonminez.storyline.sagas;

import com.yuseix.dragonminez.init.MainItems;
import com.yuseix.dragonminez.storyline.missions.Quest;
import com.yuseix.dragonminez.storyline.missions.objectives.ObjectiveCollectItem;

import java.util.List;

public class SaiyanSaga extends Saga {

	private SaiyanSaga(String id, String name) {
		super(id, name);
	}

	public static Saga createSaiyanSaga() {
		// Create the saga
		Saga saiyanSaga = new SaiyanSaga("saiyan_saga", "Saiyan Saga");

		// Add quests to the saga
		Quest quest1 = new Quest(
				"arrival_of_raditz",
				"Arrival of Raditz",
				"Raditz has landed on Earth! You must confront him.",
				List.of(new ObjectiveCollectItem(MainItems.SENZU_BEAN.get(), 5))
		);

		Quest quest2 = new Quest(
				"training_with_piccolo",
				"Training with Piccolo",
				"Prepare for the arrival of the Saiyans by training with Piccolo.",
				List.of(new ObjectiveCollectItem(MainItems.CAPSULA_MORADA.get(), 5))
		);

//		Quest quest3 = new Quest(
//				"battle_with_nappa",
//				"Battle with Nappa",
//				"The Saiyans have arrived! Defeat Nappa before he destroys the Earth.",
//				List.of("Save the civilians", "Defeat Nappa")
//		);
//
//		Quest quest4 = new Quest(
//				"final_battle_vegeta",
//				"Final Battle: Vegeta",
//				"Face Vegeta in the ultimate battle to protect the Earth.",
//				List.of("Fight Vegeta", "Survive his Great Ape transformation")
//		);

		// Add quests to the saga
		saiyanSaga.addQuest(quest1);
		saiyanSaga.addQuest(quest2);
//		saiyanSaga.addQuest(quest3);
//		saiyanSaga.addQuest(quest4);

		return saiyanSaga;
	}
}
