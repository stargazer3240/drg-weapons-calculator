package weapons.driller;

import dataGenerator.DatabaseConstants;
import guiPieces.WeaponPictures;
import guiPieces.customButtons.ButtonIcons.modIcons;
import guiPieces.customButtons.ButtonIcons.overclockIcons;
import modelPieces.EnemyInformation;
import modelPieces.Mod;
import modelPieces.Overclock;
import modelPieces.StatsRow;
import modelPieces.UtilityInformation;
import utilities.MathUtils;
import weapons.Weapon;

public class SludgePump extends Weapon {
	
	/*
		Extracted from gamefiles
		
		WPN_GooCannon
			"MassInKgOverride" 21.040535 ???????
			
		PRJ_GooProjectile_Fragment_Base
			InitialSpeed 1400
			MaxSpeed 1400
			ProjectileGravityScale 4
			
		PRJ_GooProjectile_Fragment_GooBomoberSpecial
			InitialSpeed 400
			MaxSpeed 400
			ProjectileGravityScale 3
	*/
	
	/****************************************************************************************
	* Class Variables
	****************************************************************************************/
	
	private double regularShotDamage;
	private double chargedShotDamage;
	private double chargedShotFragmentDamage;
	private int numFragmentsPerChargedShot;
	private double chargedShotWindup;
	private int ammoPerChargedShot;
	private int carriedAmmo;
	private int magazineSize;
	protected double rateOfFire;
	private double reloadTime;
	private double projectileVelocity;
	private double regularShotPuddleRadius;
	private double chargedShotPuddleRadius;
	private double corrosiveDoTDuration;
	private double puddleLifetime;
	
	// Used in SludgePump_Charged
	protected double probabilityFragmentHitsNewEnemy;
	
	/****************************************************************************************
	* Constructors
	****************************************************************************************/
	
	// Shortcut constructor to get baseline data
	public SludgePump() {
		this(-1, -1, -1, -1, -1, -1);
	}
	
	// Shortcut constructor to quickly get statistics about a specific build
	public SludgePump(String combination) {
		this(-1, -1, -1, -1, -1, -1);
		buildFromCombination(combination);
	}
	
	public SludgePump(int mod1, int mod2, int mod3, int mod4, int mod5, int overclock) {
		fullName = "Corrosive Sludge Pump";
		weaponPic = WeaponPictures.flamethrower;
		
		// Base stats, before mods or overclocks alter them:
		regularShotDamage = 25;
		chargedShotDamage = 50;
		chargedShotFragmentDamage = 5;
		numFragmentsPerChargedShot = 8;
		chargedShotWindup = 1.0;
		ammoPerChargedShot = 5;
		carriedAmmo = 100;
		magazineSize = 25;
		rateOfFire = 2.0;
		reloadTime = 3.0;
		// From reading the gamefiles, it appears that the Charged Shot inherits the projectile velocity from the base shot.
		projectileVelocity = 15.0;  // m/sec
		regularShotPuddleRadius = 0.47;
		chargedShotPuddleRadius = 0.82;
		corrosiveDoTDuration = 4.0;
		puddleLifetime = 12.0;
		
		// This number is completely arbitrary.
		probabilityFragmentHitsNewEnemy = 0.5;
		
		// Override default 10m distance for ChargedShot model of OC "Sludge Blast"
		accEstimator.setDistance(12.0);
		
		initializeModsAndOverclocks();
		// Grab initial values before customizing mods and overclocks
		setBaselineStats();
		
		// Selected Mods
		selectedTier1 = mod1;
		selectedTier2 = mod2;
		selectedTier3 = mod3;
		selectedTier4 = mod4;
		selectedTier5 = mod5;
		
		// Overclock slot
		selectedOverclock = overclock;
	}
	
	@Override
	protected void initializeModsAndOverclocks() {
		tier1 = new Mod[3];
		tier1[0] = new Mod("High Capacity Tanks", "x2 Magazine Size", modIcons.magSize, 1, 0);
		tier1[1] = new Mod("Better Air Pressurizer", "+33% Projectile Velocity", modIcons.projectileVelocity, 1, 1);
		tier1[2] = new Mod("Air Sensitive Compound", "Increases Sludge Puddle width by x1.33 and height by x1.1 (works on Large Puddles, bugged to do +10% width and +33% height on Small Puddles?)", modIcons.aoeRadius, 1, 2);
		
		tier2 = new Mod[2];
		tier2[0] = new Mod("Dyse Nozzle", "+25 Charged Shot Area Damage", modIcons.directDamage, 2, 0);
		tier2[1] = new Mod("Atomizer Nozzle", "+4 Charged Shot Fragments", modIcons.aoeRadius, 2, 1);
		
		tier3 = new Mod[3];
		tier3[0] = new Mod("Supersaturation", "x1.5 Corrosive DoT duration, x1.5 Sludge Puddle duration", modIcons.hourglass, 3, 0);
		tier3[1] = new Mod("Potent Goo Mix", "+10 Regular Shot Area Damage", modIcons.directDamage, 3, 1);
		tier3[2] = new Mod("More Goo Cannisters", "+50 Max Ammo", modIcons.carriedAmmo, 3, 2);
		
		tier4 = new Mod[2];
		tier4[0] = new Mod("Spillback Extension", "-1 Ammo per Charged Shot", modIcons.fuel, 4, 0);
		tier4[1] = new Mod("Improved Spooling Mechanism", "x0.5 Charge Time", modIcons.chargeSpeed, 4, 1);
		
		tier5 = new Mod[3];
		tier5[0] = new Mod("Protein Disruption Mix", "Increases Corrosive DoT's Slow from 35% to 51.25%, and increases the Sludge Puddle's Slow from 45% to 72.5%", modIcons.slowdown, 5, 0);
		tier5[1] = new Mod("Fluoroantimonic Acid", "Increases Corrosive DoT's average DPS by +8, and increases Sludge Puddle's average DPS by +20", modIcons.acid, 5, 1);
		tier5[2] = new Mod("Ingredient X", "The Corrosive DoT now does an average of 90 Corrosive Damage per Second to enemies' Armor (normally it can't damage Armor)", modIcons.armorBreaking, 5, 2);
		
		overclocks = new Overclock[6];
		overclocks[0] = new Overclock(Overclock.classification.clean, "Hydrogen Ion Additive", "Increases Corrosive DoT's average DPS by +8, and increases Corrosive DoT's Slow from 35% to 51.25%", overclockIcons.acid, 0);
		overclocks[1] = new Overclock(Overclock.classification.clean, "AG Mixture", "+15% Projectile Velocity, x0.25 Gravity on projectiles, and decreases the projectiles' launch angle from 9 degrees to 3.", overclockIcons.projectileVelocity, 1);
		overclocks[2] = new Overclock(Overclock.classification.balanced, "Volatile Impact Mixture", "+10 Regular Shot Area Damage, +20 Charged Shot Area Damage, x0.5 Corrosive DoT duration, x0.5 Sludge Puddle duration", overclockIcons.directDamage, 2);
		overclocks[3] = new Overclock(Overclock.classification.balanced, "Disperser Compound", "+6 Charged Shot Fragments, +5 Fragment Area Damage, -20 Charged Shot Area Damage", overclockIcons.areaDamage, 3);
		overclocks[4] = new Overclock(Overclock.classification.unstable, "Goo Bomber Special", "Charged Shots now drop their Fragments straight down while flying, instead of upon impact. Every Fragment dropped reduces the "
				+ "damage of the main projectile. After every Fragment has been dropped, the main projectile is destroyed. Additionally: +5 Fragment Area Damage, x1.5 Charged Shot Fragments, x1.33 Sludge Puddle duration", overclockIcons.special, 4);
		overclocks[5] = new Overclock(Overclock.classification.unstable, "Sludge Blast", "Changes the Charged Shot into a \"shotgun\" blast of the Fragments instead of a larger projectile. Fragments use the Charged Shot's damage instead. Additionally: +100% Charged Shot Velocity, "
				+ "x0.4 Charged Shot Area Damage, x1.2 Charge Time, x0.8 Magazine Size, x0.8 Max Ammo, +0.6 Reload Time, and decreases the charged projectiles' launch angle from 9 degrees to 3.", overclockIcons.numPellets2, 5);
		
		// This boolean flag has to be set to True in order for Weapon.isCombinationValid() and Weapon.buildFromCombination() to work.
		modsAndOCsInitialized = true;
	}
	
	@Override
	public SludgePump clone() {
		return new SludgePump(selectedTier1, selectedTier2, selectedTier3, selectedTier4, selectedTier5, selectedOverclock);
	}
	
	public String getDwarfClass() {
		return "Driller";
	}
	public String getSimpleName() {
		return "SludgePump_RegularShot";
	}
	public int getDwarfClassID() {
		return DatabaseConstants.drillerCharacterID;
	}
	public int getWeaponID() {
		return DatabaseConstants.sludgePumpGunsID;
	}
	
	/****************************************************************************************
	* Setters and Getters
	****************************************************************************************/
	
	private double getRegularShotAreaDamage() {
		double toReturn = regularShotDamage;
		
		if (selectedTier3 == 1) {
			toReturn += 10;
		}
		
		if (selectedOverclock == 2) {
			toReturn += 10;
		}
		
		return toReturn;
	}
	protected double getChargedShotAreaDamage() {
		double toReturn = chargedShotDamage;
		
		if (selectedTier2 == 0) {
			toReturn += 25;
		}
		
		if (selectedOverclock == 2) {
			toReturn += 20;
		}
		else if (selectedOverclock == 3) {
			toReturn -= 20;
		}
		else if (selectedOverclock == 5) {
			toReturn *= 0.4;
		}
		
		return toReturn;
	}
	protected double getFragmentDamage() {
		double toReturn = chargedShotFragmentDamage;
		
		if (selectedOverclock == 3 || selectedOverclock == 4) {
			toReturn += 5;
		}
		
		return toReturn;
	}
	protected int getNumberOfFragmentsPerChargedShot() {
		int toReturn = numFragmentsPerChargedShot;
		
		if (selectedTier2 == 1) {
			toReturn += 4;
		}
		
		if (selectedOverclock == 3) {
			toReturn += 6;
		}
		else if (selectedOverclock == 4) {
			toReturn = (int) Math.round(toReturn * 1.5);
		}
		
		return toReturn;
	}
	protected double getChargeTime() {
		double toReturn = chargedShotWindup;
		
		if (selectedTier4 == 1) {
			toReturn *= 0.5;
		}
		
		if (selectedOverclock == 5) {
			toReturn *= 1.2;
		}
		
		return toReturn;
	}
	protected int getAmmoPerChargedShot() {
		int toReturn = ammoPerChargedShot;
		
		if (selectedTier4 == 0) {
			toReturn -= 1;
		}
		
		return toReturn;
	}
	protected int getCarriedAmmo() {
		double toReturn = carriedAmmo;
		
		if (selectedTier3 == 2) {
			toReturn += 50;
		}
		
		if (selectedOverclock == 5) {
			toReturn *= 0.8;
		}
		
		return (int) Math.round(toReturn);
	}
	protected int getMagazineSize() {
		double toReturn = magazineSize;
		
		if (selectedTier1 == 0) {
			toReturn *= 2;
		}
		
		if (selectedOverclock == 5) {
			toReturn *= 0.8;
		}
		
		return (int) Math.round(toReturn);
	}
	protected double getReloadTime() {
		double toReturn = reloadTime;
		
		if (selectedOverclock == 5) {
			toReturn += 0.6;
		}
		
		return toReturn;
	}
	private double getRegularProjectileVelocity() {
		double modifier = 1.0;
		
		if (selectedTier1 == 1) {
			modifier += 0.33;
		}
		
		if (selectedOverclock == 1) {
			modifier += 0.15;
		}
		
		return projectileVelocity * modifier;
	}
	protected double getChargedProjectileVelocity() {
		double modifier = 1.0;
		
		if (selectedTier1 == 1) {
			modifier += 0.33;
		}
		
		if (selectedOverclock == 1) {
			modifier += 0.15;
		}
		else if (selectedOverclock == 5) {
			modifier += 1.0;
		}
		
		return projectileVelocity * modifier;
	}
	protected double getSmallPuddleRadius() {
		double toReturn = regularShotPuddleRadius;
		
		if (selectedTier1 == 2) {
			// TODO DRG-4164; the 10% height and 33% width are being applied opposite, so small puddles only get 10% wider instead of 33%.
			// This really hurts T1.C and OC "Goo Bomber Special" in my modeling...
			toReturn *= 1.1;
		}
		
		return toReturn;
	}
	protected double getLargePuddleRadius() {
		double toReturn = chargedShotPuddleRadius;
		
		if (selectedTier1 == 2) {
			// TODO DRG-4164; large puddles seem to get 33% wider as intended.
			toReturn *= 1.33;
		}
		
		return toReturn;
	}
	protected double getCorrosiveDoTDPS() {
		double damagePerTick = 4;
		
		if (selectedTier5 == 1) {
			damagePerTick += 2;
		}
		
		if (selectedOverclock == 0) {
			damagePerTick += 2;
		}
		
		return damagePerTick * 2.0 / (0.2 + 0.3);
	}
	protected double getCorrosiveDoTSlowMultiplier() {
		// From STE_GooProjectile_GC
		double multiplier = 0.65;
		
		if (selectedTier5 == 0) {
			multiplier *= 0.75;
		}
		
		if (selectedOverclock == 0) {
			multiplier *= 0.75;
		}
		
		return multiplier;
	}
	protected double getCorrosiveDoTDuration() {
		double toReturn = corrosiveDoTDuration;
		
		if (selectedTier3 == 0) {
			toReturn *= 1.5;
		}
		
		if (selectedOverclock == 2) {
			toReturn *= 0.5;
		}
		
		return toReturn;
	}
	protected double getSludgePuddleDPS() {
		// From STE_GooProjectile_GC
		double baseDPS = 3.0 * 2.0 / (0.2 + 0.25);
		
		double extraDPS = 0;
		if (selectedTier5 == 1) {
			// With this mod, the Puddle applies a second DoT: STE_GooPuddle_ImprovedPoison
			extraDPS = 5.0 * 2.0 / (0.2 + 0.3);
		}
		
		return baseDPS + extraDPS;
	}
	protected double getSludgePuddleSlowMultiplier() {
		// From STE_GooPuddle_GC
		double multiplier = 0.55;
		
		if (selectedTier5 == 0) {
			// With this mod, the Puddle applies a second slow: STE_GooPuddle_ImprovedSlow
			multiplier *= 0.5;
		}
		
		return multiplier;
	}
	protected double getSludgePuddleDuration() {
		double toReturn = puddleLifetime;
		
		if (selectedTier3 == 0) {
			toReturn *= 1.5;
		}
		
		if (selectedOverclock == 2) {
			toReturn *= 0.5;
		}
		else if (selectedOverclock == 4) {
			toReturn *= 1.33;
		}
		
		return toReturn;
	}
	
	@Override
	public StatsRow[] getStats() {
		StatsRow[] toReturn = new StatsRow[14];
		
		toReturn[0] = new StatsRow("Regular Shot Area Damage:", getRegularShotAreaDamage(), modIcons.areaDamage, selectedTier3 == 1 || selectedOverclock == 2);
		toReturn[1] = new StatsRow("Regular Shot AoE Radius:", aoeEfficiency[0], modIcons.aoeRadius, false);
		toReturn[2] = new StatsRow("Regular Shot Velocity:", getRegularProjectileVelocity(), modIcons.projectileVelocity, selectedTier1 == 1 || selectedOverclock == 1);
		toReturn[3] = new StatsRow("Magazine Size:", getMagazineSize(), modIcons.magSize, selectedTier1 == 0 || selectedOverclock == 5);
		toReturn[4] = new StatsRow("Max Ammo:", getCarriedAmmo(), modIcons.carriedAmmo, selectedTier3 == 2 || selectedOverclock == 5);
		toReturn[5] = new StatsRow("Rate of Fire:", rateOfFire, modIcons.rateOfFire, false);
		toReturn[6] = new StatsRow("Reload Time:", getReloadTime(), modIcons.reloadSpeed, selectedOverclock == 5);
		
		toReturn[7] = new StatsRow("Small Puddle Width:", 2.0 * getSmallPuddleRadius(), modIcons.aoeRadius, selectedTier1 == 2);
		
		toReturn[8] = new StatsRow("Corrosive DoT DPS:", getCorrosiveDoTDPS(), modIcons.acid, selectedTier5 == 1 || selectedOverclock == 0);
		toReturn[9] = new StatsRow("Corrosive DoT Slow:", convertDoubleToPercentage(1.0 - getCorrosiveDoTSlowMultiplier()), modIcons.slowdown, selectedTier5 == 0 || selectedOverclock == 0);
		toReturn[10] = new StatsRow("Corrosive DoT Duration:", getCorrosiveDoTDuration(), modIcons.hourglass, selectedTier3 == 0 || selectedOverclock == 2);
		toReturn[11] = new StatsRow("Sludge Puddle DPS:", getSludgePuddleDPS(), modIcons.acid, selectedTier5 == 1);
		toReturn[12] = new StatsRow("Sludge Puddle Slow:", convertDoubleToPercentage(1.0 - getSludgePuddleSlowMultiplier()), modIcons.slowdown, selectedTier5 == 0);
		boolean puddleDurationModified = selectedTier3 == 0 || selectedOverclock == 2 || selectedOverclock == 4;
		toReturn[13] = new StatsRow("Sludge Puddle Duration:", getSludgePuddleDuration(), modIcons.hourglass, puddleDurationModified);
		
		return toReturn;
	}
	
	/****************************************************************************************
	* Other Methods
	****************************************************************************************/
	
	@Override
	public boolean currentlyDealsSplashDamage() {
		return true;
	}
	
	@Override
	protected void setAoEEfficiency() {
		aoeEfficiency = calculateAverageAreaDamage(1.0, 0.5, 0.25);
	}

	// Single-target calculations
	@Override
	public double calculateSingleTargetDPS(boolean burst, boolean weakpoint, boolean accuracy, boolean armorWasting) {
		// The only flag that actually does anything for Sludge Pump is 'burst'. It doesn't do WP damage, it's not affected by accuracy, and doesn't get reduced by Armor.
		
		double magSize = getMagazineSize();
		double duration;
		if (burst) {
			duration = magSize / rateOfFire;
		}
		else {
			duration = magSize / rateOfFire + getReloadTime();
		}
		
		double areaDamage = getRegularShotAreaDamage();
		
		// IFG Grenade
		if (statusEffects[3]) {
			areaDamage *= UtilityInformation.IFG_Damage_Multiplier;
		}
		
		return areaDamage * magSize / duration + getCorrosiveDoTDPS() + getSludgePuddleDPS();
	}

	// Multi-target calculations
	@Override
	public double calculateAdditionalTargetDPS() {
		double magSize = getMagazineSize();
		double duration = magSize / rateOfFire + getReloadTime();
		double areaDamage = getRegularShotAreaDamage() * aoeEfficiency[1];
		
		return areaDamage * magSize / duration + getCorrosiveDoTDPS() + getSludgePuddleDPS();
	}

	@Override
	public double calculateMaxMultiTargetDamage() {
		// Damage dealt by Corrosive DoTs
		double corrosiveDoTDamagePerEnemy = calculateAverageDoTDamagePerEnemy(0, getCorrosiveDoTDuration(), getCorrosiveDoTDPS());
		double totalCorrosiveDoTDamage = corrosiveDoTDamagePerEnemy * aoeEfficiency[2];
		
		// Damage dealt by Sludge Puddles
		double sludgePuddleDamagePerEnemy = calculateAverageDoTDamagePerEnemy(0, getSludgePuddleDPS(), getSludgePuddleDuration());
		double totalSludgePuddleDamage = sludgePuddleDamagePerEnemy * calculateNumGlyphidsInRadius(getSmallPuddleRadius(), false);
		
		double avgTTK = averageTimeToKill();
		double firingDuration = calculateFiringDuration();
		return getRegularShotAreaDamage() * aoeEfficiency[2] * aoeEfficiency[1] * (getMagazineSize() + getCarriedAmmo()) + (totalCorrosiveDoTDamage + totalSludgePuddleDamage) * (firingDuration / avgTTK);
	}

	@Override
	public int calculateMaxNumTargets() {
		return (int) aoeEfficiency[2];
	}

	@Override
	public double calculateFiringDuration() {
		int magSize = getMagazineSize();
		int carriedAmmo = getCarriedAmmo();
		double timeToFireMagazine = ((double) magSize) / rateOfFire;
		return numMagazines(carriedAmmo, magSize) * timeToFireMagazine + numReloads(carriedAmmo, magSize) * getReloadTime();
	}
	
	@Override
	protected double averageDamageToKillEnemy() {
		double dmgPerShot = getRegularShotAreaDamage();
		return Math.ceil(EnemyInformation.averageHealthPool() / dmgPerShot) * dmgPerShot;
	}
	
	@Override
	public double averageOverkill() {
		overkillPercentages = EnemyInformation.overkillPerCreature(getRegularShotAreaDamage());
		return MathUtils.vectorDotProduct(overkillPercentages[0], overkillPercentages[1]);
	}

	@Override
	public double estimatedAccuracy(boolean weakpointAccuracy) {
		// This stat is only applicable to "gun"-type weapons
		/* 
			BaseSpread 1.0
			
			RecoilPitch 30
			RecoilYaw 30
			CanRecoilDown True
			SpringStiffness 145
			Mass 1.6
		*/
		return -1.0;
	}
	
	@Override
	public int breakpoints() {
		// Sludge Gun blobs don't need to be counted for Breakpoint calculations
		// All Area Damage; 75% Kinetic / 25% Corrosive
		return 0;
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
		double numEnemiesHitByRegularShotAoE = aoeEfficiency[2];
		double numEnemiesHitBySmallPuddle = calculateNumGlyphidsInRadius(getSmallPuddleRadius(), false);
		utilityScores[3] = numEnemiesHitByRegularShotAoE * getCorrosiveDoTDuration() * (1.0 - getCorrosiveDoTSlowMultiplier()) + numEnemiesHitBySmallPuddle * getSludgePuddleDuration() * (1.0 - getSludgePuddleSlowMultiplier());
		
		return MathUtils.sum(utilityScores);
	}
	
	@Override
	public double averageTimeToCauterize() {
		// This weapon can't ignite or freeze enemies by itself.
		return -1;
	}
	
	@Override
	public double damagePerMagazine() {
		// Shamelessly copy/pasting from MaxMultiTargetDamage()
		// Damage dealt by Corrosive DoTs
		double corrosiveDoTDamagePerEnemy = calculateAverageDoTDamagePerEnemy(0, getCorrosiveDoTDuration(), getCorrosiveDoTDPS());
		double totalCorrosiveDoTDamage = corrosiveDoTDamagePerEnemy * aoeEfficiency[2];
		
		// Damage dealt by Sludge Puddles
		double sludgePuddleDamagePerEnemy = calculateAverageDoTDamagePerEnemy(0, getSludgePuddleDPS(), getSludgePuddleDuration());
		double totalSludgePuddleDamage = sludgePuddleDamagePerEnemy * calculateNumGlyphidsInRadius(getSmallPuddleRadius(), false);
		
		double avgTTK = averageTimeToKill();
		double firingDuration = timeToFireMagazine();
		return getRegularShotAreaDamage() * aoeEfficiency[2] * aoeEfficiency[1] * getMagazineSize() + (totalCorrosiveDoTDamage + totalSludgePuddleDamage) * (firingDuration / avgTTK);
	}
	
	@Override
	public double timeToFireMagazine() {
		return getMagazineSize() / rateOfFire;
	}
	
	@Override
	public double damageWastedByArmor() {
		return 0;
	}
}
