package weapons.driller;

import guiPieces.customButtons.ButtonIcons.modIcons;
import modelPieces.EnemyInformation;
import modelPieces.StatsRow;
import modelPieces.UtilityInformation;
import utilities.MathUtils;

public class SludgePump_Charged extends SludgePump {
	
	// TODO: Goo Bomber Special is scaling with T1.B in ways that I can't figure out yet.
	
	/****************************************************************************************
	* Constructors
	****************************************************************************************/
	
	// Shortcut constructor to get baseline data
	public SludgePump_Charged() {
		this(-1, -1, -1, -1, -1, -1);
	}
	
	// Shortcut constructor to quickly get statistics about a specific build
	public SludgePump_Charged(String combination) {
		this(-1, -1, -1, -1, -1, -1);
		buildFromCombination(combination);
	}
	
	public SludgePump_Charged(int mod1, int mod2, int mod3, int mod4, int mod5, int overclock) {
		super(mod1, mod2, mod3, mod4, mod5, overclock);
		fullName = "Corrosive Sludge Pump (Charged Shots)";
	}
	
	@Override
	public SludgePump_Charged clone() {
		return new SludgePump_Charged(selectedTier1, selectedTier2, selectedTier3, selectedTier4, selectedTier5, selectedOverclock);
	}
	
	public String getSimpleName() {
		return "SludgePump_Charged";
	}
	
	/****************************************************************************************
	* Setters and Getters
	****************************************************************************************/
	
	@Override
	public double getRateOfFire() {
		// The time it takes to charge up, fire a Charged Shot, and then the 1/RoF delay before the next shot can start charging.
		return 1.0 / (getChargeTime() + 1.0 / rateOfFire);
	}
	
	@Override
	public StatsRow[] getStats() {
		StatsRow[] toReturn = new StatsRow[18];
		
		boolean chargedDamageModified = selectedTier2 == 0 || selectedOverclock == 2 || selectedOverclock == 3 || selectedOverclock == 5;
		toReturn[0] = new StatsRow("Charged Shot Area Damage:", getChargedShotAreaDamage(), modIcons.areaDamage, chargedDamageModified);
		toReturn[1] = new StatsRow("Charged Shot AoE Radius:", aoeEfficiency[0], modIcons.aoeRadius, selectedOverclock == 5);
		toReturn[2] = new StatsRow("Charged Shot Windup:", getChargeTime(), modIcons.duration, selectedTier4 == 1 || selectedOverclock == 5);
		toReturn[3] = new StatsRow("Ammo/Charged Shot:", getAmmoPerChargedShot(), modIcons.fuel, selectedTier4 == 0);
		boolean velocityModified = selectedTier1 == 1 || selectedOverclock == 1 || selectedOverclock == 5;
		toReturn[4] = new StatsRow("Charged Shot Velocity:", getChargedProjectileVelocity(), modIcons.projectileVelocity, velocityModified);
		toReturn[5] = new StatsRow("Fragment Area Damage:", getFragmentDamage(), modIcons.areaDamage, selectedOverclock == 3 || selectedOverclock == 4, selectedOverclock != 5);
		boolean fragmentCountModified = selectedTier2 == 1 || selectedOverclock == 3 || selectedOverclock == 4;
		toReturn[6] = new StatsRow("Number of Fragments:", getNumberOfFragmentsPerChargedShot(), modIcons.pelletsPerShot, fragmentCountModified);
		
		toReturn[7] = new StatsRow("Magazine Size:", getMagazineSize(), modIcons.magSize, selectedTier1 == 0 || selectedOverclock == 5);
		toReturn[8] = new StatsRow("Max Ammo:", getCarriedAmmo(), modIcons.carriedAmmo, selectedTier3 == 2 || selectedOverclock == 5);
		toReturn[9] = new StatsRow("Rate of Fire:", getRateOfFire(), modIcons.rateOfFire, selectedTier4 == 1 || selectedOverclock == 5);
		toReturn[10] = new StatsRow("Reload Time:", getReloadTime(), modIcons.reloadSpeed, selectedOverclock == 5);
		
		toReturn[11] = new StatsRow("Large Puddle Width:", 2.0 * getLargePuddleRadius(), modIcons.aoeRadius, selectedTier1 == 2, selectedOverclock < 4);
		
		toReturn[12] = new StatsRow("Corrosive DoT DPS:", getCorrosiveDoTDPS(), modIcons.acid, selectedTier5 == 1 || selectedOverclock == 0);
		toReturn[13] = new StatsRow("Corrosive DoT Slow:", convertDoubleToPercentage(1.0 - getCorrosiveDoTSlowMultiplier()), modIcons.slowdown, selectedTier5 == 0 || selectedOverclock == 0);
		toReturn[14] = new StatsRow("Corrosive DoT Duration:", getCorrosiveDoTDuration(), modIcons.hourglass, selectedTier3 == 0 || selectedOverclock == 2);
		toReturn[15] = new StatsRow("Sludge Puddle DPS:", getSludgePuddleDPS(), modIcons.acid, selectedTier5 == 1);
		toReturn[16] = new StatsRow("Sludge Puddle Slow:", convertDoubleToPercentage(1.0 - getSludgePuddleSlowMultiplier()), modIcons.slowdown, selectedTier5 == 0);
		boolean puddleDurationModified = selectedTier3 == 0 || selectedOverclock == 2 || selectedOverclock == 4;
		toReturn[17] = new StatsRow("Sludge Puddle Duration:", getSludgePuddleDuration(), modIcons.hourglass, puddleDurationModified);
		
		return toReturn;
	}
	
	/****************************************************************************************
	* Other Methods
	****************************************************************************************/
	
	private double calculateGooBomberSpecialPathLength() {
		/*
			From observation, it seems like the length of this path is equal to the number of fragments dropped times the width of the puddles.
			It scales both with T1.C making the small puddles 10% wider (bug; DRG4164; should be 33% wider) as well as T2.B adding more fragments.
		*/
		// TODO: this is greatly affected by T1.B's +33% velocity. don't know how yet.
		return getNumberOfFragmentsPerChargedShot() * getSmallPuddleRadius() * 2.0;
	}
	
	@Override
	protected void setAoEEfficiency() {
		// OC "Goo Bomber Special" changes the weapon to drop the PRJ_Fragment_GooBobomerSpecial projectiles which reduces AoE back down to the normal shot.
		// OC "Sludge Blast" changes the projectiles to PRJ_BigGoo_Buckshot which reduces AoE back down to the normal shot.
		if (selectedOverclock > 3) {
			aoeEfficiency = calculateAverageAreaDamage(1.0, 0.5, 0.25);
		}
		else {
			aoeEfficiency = calculateAverageAreaDamage(2.5, 1.0, 0.25);
		}
	}
	
	// Single-target calculations
	@Override
	public double calculateSingleTargetDPS(boolean burst, boolean weakpoint, boolean accuracy, boolean armorWasting) {
		double duration, baseDPS;
		double numChargedShots = Math.ceil(getMagazineSize() / (double) getAmmoPerChargedShot());
		
		if (burst) {
			duration = numChargedShots / getRateOfFire();
		}
		else {
			duration = numChargedShots / getRateOfFire() + getReloadTime();
		}
		
		if (selectedOverclock == 4) {
			// OC "Goo Bomber Special" fundamentally changes how this number gets calculated
			baseDPS = getFragmentDamage() * numChargedShots / duration;
		}
		else if (selectedOverclock == 5) {
			// OC "Sludge Blast" fundamentally changes how this number gets calculated
			double damagePerChargedShot = getChargedShotAreaDamage() * getNumberOfFragmentsPerChargedShot() * (estimatedAccuracy(false) / 100.0);
			baseDPS = damagePerChargedShot * numChargedShots / duration;
		}
		else {
			// Normal Charged Shots behavior -- hitting the center and then throwing fragments around
			// Choosing to model primary-target DPS as if only 1 Fragment re-hits the primary enemy, from hitting the projectile right beneath/in-front of the primary target
			baseDPS = (getChargedShotAreaDamage() + getFragmentDamage()) * numChargedShots / duration;
		}
		
		// Because the Corrosive DoT and Sludge DoT DPS both start as soon as the enemy gets hit by a projectile, they don't change between Burst/Sustained calculations.
		return baseDPS + getCorrosiveDoTDPS() + getSludgePuddleDPS();
	}

	@Override
	public double calculateAdditionalTargetDPS() {
		double numChargedShots = Math.ceil(getMagazineSize() / (double) getAmmoPerChargedShot());
		double duration = numChargedShots / getRateOfFire() + getReloadTime();
		
		double baseDPS;
		if (selectedOverclock == 4) {
			// OC "Goo Bomber Special" fundamentally changes how this number gets calculated
			baseDPS = getFragmentDamage() * numChargedShots / duration;
		}
		else if (selectedOverclock == 5) {
			// OC "Sludge Blast" fundamentally changes how this number gets calculated
			double damagePerChargedShot = getChargedShotAreaDamage() * aoeEfficiency[1];
			baseDPS = damagePerChargedShot * numChargedShots / duration;
		}
		else {
			// Normal Charged Shots behavior -- hitting the center and then throwing fragments around
			// Choosing to model secondary target DPS as if only 1 Fragment hits it directly
			baseDPS = (getChargedShotAreaDamage() * aoeEfficiency[1] + getFragmentDamage()) * numChargedShots / duration;
		}
		
		return baseDPS + getCorrosiveDoTDPS() + getSludgePuddleDPS();
	}

	@Override
	public double calculateMaxMultiTargetDamage() {
		int numFragmentsTotal = getNumberOfFragmentsPerChargedShot();
		// TODO: the number of enemies hit by puddles doesn't change when T1.C gets equipped... not sure the best way to model it right now.
		double enemiesHitBySmallPuddles = calculateNumGlyphidsInRadius(getSmallPuddleRadius(), false);
		
		// Area Damage dealt per Charged Shot
		double areaDamagePerChargedShot;
		double numEnemiesHitByFragments, numEnemiesCorrosiveDoTGetsAppliedTo;
		double numEnemiesInLargePuddle = 0, numEnemiesInSmallPuddles;
		if (selectedOverclock == 4) {
			// OC "Goo Bomber Special" fundamentally changes how this number gets calculated
			numEnemiesHitByFragments = calculateNumGlyphidsInStream(calculateGooBomberSpecialPathLength());
			areaDamagePerChargedShot = numEnemiesHitByFragments * getFragmentDamage() * aoeEfficiency[2] * aoeEfficiency[1];  // TODO: this might need to be scaled back?
			
			// Corrosive DoTs
			numEnemiesCorrosiveDoTGetsAppliedTo = numEnemiesHitByFragments;
			
			// Sludge Puddles
			numEnemiesInSmallPuddles = numEnemiesHitByFragments * enemiesHitBySmallPuddles;
		}
		else if (selectedOverclock == 5) {
			// OC "Sludge Blast" fundamentally changes how this number gets calculated
			double projectilesAccuracy = estimatedAccuracy(false) / 100.0;
			numEnemiesHitByFragments = Math.round(projectilesAccuracy * numFragmentsTotal);
			areaDamagePerChargedShot = numEnemiesHitByFragments * getChargedShotAreaDamage() * aoeEfficiency[2] * aoeEfficiency[1];  // TODO: this might need to be scaled back?
			
			// Corrosive DoTs
			numEnemiesCorrosiveDoTGetsAppliedTo = numEnemiesHitByFragments;
			
			// Sludge Puddles
			double puddlesAccuracy = 1.0 - (estimatedAccuracy(true) / 100.0);
			numEnemiesInSmallPuddles = Math.round(puddlesAccuracy * numFragmentsTotal) * enemiesHitBySmallPuddles;
		}
		else {
			// Normal Charged Shots behavior -- hitting the center and then throwing fragments around
			numEnemiesHitByFragments = Math.round(probabilityFragmentHitsNewEnemy * numFragmentsTotal);
			areaDamagePerChargedShot = getChargedShotAreaDamage() * aoeEfficiency[1] * aoeEfficiency[2] + numEnemiesHitByFragments * getFragmentDamage();
			
			// Corrosive DoTs
			numEnemiesCorrosiveDoTGetsAppliedTo = aoeEfficiency[2] + numEnemiesHitByFragments;
			
			// Sludge Puddles
			numEnemiesInLargePuddle = calculateNumGlyphidsInRadius(getLargePuddleRadius(), false);
			numEnemiesInSmallPuddles = Math.round((1.0 - probabilityFragmentHitsNewEnemy) * numFragmentsTotal) * enemiesHitBySmallPuddles;
		}
		
		// Damage dealt by Corrosive DoTs
		double corrosiveDoTDamagePerEnemy = calculateAverageDoTDamagePerEnemy(0, getCorrosiveDoTDuration(), getCorrosiveDoTDPS());
		double totalCorrosiveDoTDamage = corrosiveDoTDamagePerEnemy * numEnemiesCorrosiveDoTGetsAppliedTo;
		
		// Damage dealt by Sludge Puddles
		double sludgePuddleDamagePerEnemy = calculateAverageDoTDamagePerEnemy(0, getSludgePuddleDPS(), getSludgePuddleDuration());
		double totalSludgePuddleDamage = sludgePuddleDamagePerEnemy * (numEnemiesInLargePuddle + numEnemiesInSmallPuddles);
		
		// Finally, the actual calculation!
		double avgTTK = averageTimeToKill();
		double firingDuration = calculateFiringDuration();
		
		int magSize = getMagazineSize();
		int carriedAmmo = getCarriedAmmo();
		double numChargedShotsPerMag = Math.ceil(magSize / (double) getAmmoPerChargedShot());
		double totalNumChargedShots = numChargedShotsPerMag * numMagazines(carriedAmmo, magSize);
		
		return totalNumChargedShots * areaDamagePerChargedShot + (totalCorrosiveDoTDamage + totalSludgePuddleDamage) * (firingDuration / avgTTK);
	}

	@Override
	public int calculateMaxNumTargets() {
		int numFragmentsTotal = getNumberOfFragmentsPerChargedShot();
		
		if (selectedOverclock == 4) {
			// OC "Goo Bomber Special" fundamentally changes how this number gets calculated
			return (int) calculateNumGlyphidsInStream(calculateGooBomberSpecialPathLength());
		}
		else if (selectedOverclock == 5) {
			// OC "Sludge Blast" fundamentally changes how this number gets calculated
			double projectilesAccuracy = estimatedAccuracy(false) / 100.0;
			return (int) Math.round(projectilesAccuracy * numFragmentsTotal);
		}
		else {
			// Normal Charged Shots behavior -- hitting the center and then throwing fragments around
			return (int) (aoeEfficiency[2] + Math.round(probabilityFragmentHitsNewEnemy * numFragmentsTotal));
		}
	}

	@Override
	public double calculateFiringDuration() {
		int magSize = getMagazineSize();
		int carriedAmmo = getCarriedAmmo();
		double numChargedShotsPerMag = Math.ceil(magSize / (double) getAmmoPerChargedShot());
		return numMagazines(carriedAmmo, magSize) * numChargedShotsPerMag / getRateOfFire() + numReloads(carriedAmmo, magSize) * getReloadTime();
	}
	
	@Override
	protected double averageDamageToKillEnemy() {
		double dmgPerShot = getChargedShotAreaDamage();
		return Math.ceil(EnemyInformation.averageHealthPool() / dmgPerShot) * dmgPerShot;
	}
	
	@Override
	public double averageOverkill() {
		overkillPercentages = EnemyInformation.overkillPerCreature(getChargedShotAreaDamage());
		return MathUtils.vectorDotProduct(overkillPercentages[0], overkillPercentages[1]);
	}
	
	@Override
	public double estimatedAccuracy(boolean weakpointAccuracy) {
		// This stat is only usable for OC "Sludge Blast"
		/* 
			BuckShotSpreadV 4
			BuckShotSpreadH 8
			
			RecoilPitch 30
			RecoilYaw 30
			CanRecoilDown True
			SpringStiffness 145
			Mass 1.6
		*/
		if (selectedOverclock == 5) {
			// The distance is set to 12m in SludgePump's constructor.
			return accEstimator.calculateRectangularAccuracy(weakpointAccuracy, 8.0, 4.0, 30, 30, 1.6, 145);
		}
		else {
			return -1.0;
		}
	}

	@Override
	public double utilityScore() {
		// Armor Break
		// Normally, Sludge Pump can't damage or interact with Armor in any way. The only exception is T5.C
		if (selectedTier5 == 2) {
			// The DoT that damages Armor plates can do a range of [20, 25] damage per Tick, and uses the same damage ticks as Corrosive DoT [0.2, 0.3]
			double armorDoTDPS = (20 + 25) / (0.2 + 0.3);
			utilityScores[2] = armorDoTDPS * UtilityInformation.ArmorBreak_Utility;
		}
		else {
			utilityScores[2] = 0;
		}
		
		// Slow
		// The Corrosive DoT and Sludge Puddle both slow, and their slows multiply together. However, I'm choosing to just add them here because there's like... 9 overlapping cases of interaction to model if multiplying.
		double numEnemiesThatGetCorrosiveDoT = calculateMaxNumTargets();
		
		int numFragmentsTotal = getNumberOfFragmentsPerChargedShot();
		// TODO: the number of enemies hit by puddles doesn't change when T1.C gets equipped... not sure the best way to model it right now.
		double enemiesHitBySmallPuddles = calculateNumGlyphidsInRadius(getSmallPuddleRadius(), false);
		double numEnemiesInLargePuddle = 0, numEnemiesInSmallPuddles;
		
		if (selectedOverclock == 4) {
			// OC "Goo Bomber Special" fundamentally changes how this number gets calculated
			numEnemiesInSmallPuddles = numEnemiesThatGetCorrosiveDoT * enemiesHitBySmallPuddles;
		}
		else if (selectedOverclock == 5) {
			// OC "Sludge Blast" fundamentally changes how this number gets calculated
			double puddlesAccuracy = 1.0 - (estimatedAccuracy(true) / 100.0);
			numEnemiesInSmallPuddles = Math.round(puddlesAccuracy * numFragmentsTotal) * enemiesHitBySmallPuddles;
		}
		else {
			// Normal Charged Shots behavior -- hitting the center and then throwing fragments around
			numEnemiesInLargePuddle = calculateNumGlyphidsInRadius(getLargePuddleRadius(), false);
			numEnemiesInSmallPuddles = Math.round((1.0 - probabilityFragmentHitsNewEnemy) * numFragmentsTotal) * enemiesHitBySmallPuddles;
		}
		
		utilityScores[3] = numEnemiesThatGetCorrosiveDoT * getCorrosiveDoTDuration() * (1.0 - getCorrosiveDoTSlowMultiplier()) + (numEnemiesInLargePuddle + numEnemiesInSmallPuddles) * getSludgePuddleDuration() * (1.0 - getSludgePuddleSlowMultiplier());
		
		return MathUtils.sum(utilityScores);
	}
	
	@Override
	public double damagePerMagazine() {
		// Again, shamelessly copy/pasting from MaxMultiTargetDamage()
		int numFragmentsTotal = getNumberOfFragmentsPerChargedShot();
		double enemiesHitBySmallPuddles = calculateNumGlyphidsInRadius(getSmallPuddleRadius(), false);
		double enemiesHitByLargePuddles = calculateNumGlyphidsInRadius(getLargePuddleRadius(), false);
		
		// Area Damage dealt per Charged Shot
		double areaDamagePerChargedShot;
		double numEnemiesHitByFragments, numEnemiesCorrosiveDoTGetsAppliedTo;
		double numEnemiesInLargePuddle = 0, numEnemiesInSmallPuddles;
		if (selectedOverclock == 4) {
			// OC "Goo Bomber Special" fundamentally changes how this number gets calculated
			numEnemiesHitByFragments = calculateNumGlyphidsInStream(calculateGooBomberSpecialPathLength());
			areaDamagePerChargedShot = numEnemiesHitByFragments * getFragmentDamage() * aoeEfficiency[2] * aoeEfficiency[1];  // TODO: this might need to be scaled back after i finish the DPS method?
			
			// Corrosive DoTs
			numEnemiesCorrosiveDoTGetsAppliedTo = numEnemiesHitByFragments;
			
			// Sludge Puddles
			numEnemiesInSmallPuddles = numEnemiesHitByFragments * enemiesHitBySmallPuddles;
		}
		else if (selectedOverclock == 5) {
			// OC "Sludge Blast" fundamentally changes how this number gets calculated
			double projectilesAccuracy = estimatedAccuracy(false) / 100.0;
			numEnemiesHitByFragments = Math.round(projectilesAccuracy * numFragmentsTotal);
			areaDamagePerChargedShot = numEnemiesHitByFragments * getChargedShotAreaDamage() * aoeEfficiency[2] * aoeEfficiency[1];  // TODO: this might need to be scaled back after i finish the DPS method?
			
			// Corrosive DoTs
			numEnemiesCorrosiveDoTGetsAppliedTo = numEnemiesHitByFragments;
			
			// Sludge Puddles
			double puddlesAccuracy = 1.0 - (estimatedAccuracy(true) / 100.0);
			numEnemiesInSmallPuddles = Math.round(puddlesAccuracy * numFragmentsTotal) * enemiesHitBySmallPuddles;
		}
		else {
			// Normal Charged Shots behavior -- hitting the center and then throwing fragments around
			numEnemiesHitByFragments = Math.round(probabilityFragmentHitsNewEnemy * numFragmentsTotal);
			areaDamagePerChargedShot = getChargedShotAreaDamage() * aoeEfficiency[1] * aoeEfficiency[2] + numEnemiesHitByFragments * getFragmentDamage();
			
			// Corrosive DoTs
			numEnemiesCorrosiveDoTGetsAppliedTo = aoeEfficiency[2] + numEnemiesHitByFragments;
			
			// Sludge Puddles
			numEnemiesInLargePuddle = enemiesHitByLargePuddles;
			numEnemiesInSmallPuddles = Math.round((1.0 - probabilityFragmentHitsNewEnemy) * numFragmentsTotal) * enemiesHitBySmallPuddles;
		}
		
		// Damage dealt by Corrosive DoTs
		double corrosiveDoTDamagePerEnemy = calculateAverageDoTDamagePerEnemy(0, getCorrosiveDoTDuration(), getCorrosiveDoTDPS());
		double totalCorrosiveDoTDamage = corrosiveDoTDamagePerEnemy * numEnemiesCorrosiveDoTGetsAppliedTo;
		
		// Damage dealt by Sludge Puddles
		double sludgePuddleDamagePerEnemy = calculateAverageDoTDamagePerEnemy(0, getSludgePuddleDPS(), getSludgePuddleDuration());
		double totalSludgePuddleDamage = sludgePuddleDamagePerEnemy * (numEnemiesInLargePuddle + numEnemiesInSmallPuddles);
		
		// Finally, the actual calculation!
		double avgTTK = averageTimeToKill();
		double firingDuration = timeToFireMagazine();
		
		int magSize = getMagazineSize();
		double numChargedShotsPerMag = Math.ceil(magSize / (double) getAmmoPerChargedShot());
		
		return numChargedShotsPerMag * areaDamagePerChargedShot + (totalCorrosiveDoTDamage + totalSludgePuddleDamage) * (firingDuration / avgTTK);
	}
	
	@Override
	public double timeToFireMagazine() {
		double numChargedShots = Math.ceil(getMagazineSize() / (double) getAmmoPerChargedShot());
		return numChargedShots / getRateOfFire();
	}
}
