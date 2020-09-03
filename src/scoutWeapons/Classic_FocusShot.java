package scoutWeapons;

import guiPieces.ButtonIcons.modIcons;
import modelPieces.DoTInformation;
import modelPieces.DwarfInformation;
import modelPieces.EnemyInformation;
import modelPieces.StatsRow;
import modelPieces.UtilityInformation;
import utilities.MathUtils;

/*
	The way MikeGSG explained it to me, the M1000 waits 0.2 seconds before beginning a Focused Shot to prevent it from "jittering".
	Once it starts charging, by default it only takes 0.8 seconds to fully charge, for what feels like a 1 second period to the user.
	The 0.8 duration is what gets affected by Charge Speed, not the 0.2 sec delay. Additionally, the crosshair animation doesn't begin
	until a minimum charge has been gained, so by the time the crosshair starts moving the Focus Shot has already been charging for a short time.
*/

public class Classic_FocusShot extends Classic {
	
	/****************************************************************************************
	* Class Variables
	****************************************************************************************/
	
	/****************************************************************************************
	* Constructors
	****************************************************************************************/
	
	// Shortcut constructor to get baseline data
	public Classic_FocusShot() {
		this(-1, -1, -1, -1, -1, -1);
	}
	
	// Shortcut constructor to quickly get statistics about a specific build
	public Classic_FocusShot(String combination) {
		this(-1, -1, -1, -1, -1, -1);
		buildFromCombination(combination);
	}
	
	public Classic_FocusShot(int mod1, int mod2, int mod3, int mod4, int mod5, int overclock) {
		super(mod1, mod2, mod3, mod4, mod5, overclock);
		fullName = "M1000 Classic (Focused Shots)";
	}
	
	@Override
	public Classic_FocusShot clone() {
		return new Classic_FocusShot(selectedTier1, selectedTier2, selectedTier3, selectedTier4, selectedTier5, selectedOverclock);
	}
	
	public String getSimpleName() {
		return "Classic_FocusShot";
	}
	
	/****************************************************************************************
	* Setters and Getters
	****************************************************************************************/
	
	@Override
	protected int getCarriedAmmo() {
		double toReturn = carriedAmmo;
		
		if (selectedTier1 == 0) {
			toReturn += 32;
		}
		
		if (selectedOverclock == 1) {
			toReturn += 16;
		}
		else if (selectedOverclock == 3) {
			toReturn += 72;
		}
		else if (selectedOverclock == 5) {
			toReturn *= 0.635;
		}
		
		// Divide by 2 to account for firing two ammo per focused shot
		// Use ceiling function because you can do a fully-charged Focus Shot with only 1 ammo if there's no ammo left in the clip
		return (int) Math.ceil(toReturn / 2.0);
	}
	@Override
	protected int getMagazineSize() {
		int toReturn = magazineSize;
		
		if (selectedTier3 == 1) {
			toReturn += 6;
		}
		
		// Divide by 2 to account for firing two ammo per focused shot
		return toReturn / 2;
	}
	@Override
	protected double getRateOfFire() {
		// Because the max RoF will never be achieved with Focus Shots, instead model the RoF as the inverse of the Focus Duration
		return 1.0 / (getFocusDelay() + getFocusDuration());
	}
	
	@Override
	public StatsRow[] getStats() {
		StatsRow[] toReturn = new StatsRow[14];
		
		toReturn[0] = new StatsRow("Direct Damage:", getDirectDamage(), modIcons.directDamage, selectedOverclock == 3 || selectedTier1 == 1);
		
		boolean multiplierModified = selectedTier3 == 0 || selectedOverclock == 2 || selectedOverclock == 4 || selectedOverclock == 5;
		toReturn[1] = new StatsRow("Focused Shot Multiplier:", convertDoubleToPercentage(getFocusedShotMultiplier()), modIcons.directDamage, multiplierModified);
		
		toReturn[2] = new StatsRow("Delay Before Focusing:", getFocusDelay(), modIcons.duration, selectedOverclock == 3);
		
		toReturn[3] = new StatsRow("Focus Shot Charge-up Duration:", getFocusDuration(), modIcons.chargeSpeed, selectedTier2 == 0 || selectedOverclock == 5);
		
		toReturn[4] = new StatsRow("Clip Size:", getMagazineSize(), modIcons.magSize, selectedTier3 == 1);
		
		boolean carriedAmmoModified = selectedTier1 == 0 || selectedOverclock == 1 || selectedOverclock == 3 || selectedOverclock == 5;
		toReturn[5] = new StatsRow("Max Ammo:", getCarriedAmmo(), modIcons.carriedAmmo, carriedAmmoModified);
		
		toReturn[6] = new StatsRow("Rate of Fire:", getRateOfFire(), modIcons.rateOfFire, selectedTier2 == 0 || selectedOverclock == 5);
		
		toReturn[7] = new StatsRow("Reload Time:", getReloadTime(), modIcons.reloadSpeed, selectedTier5 == 2 || selectedOverclock == 1);
		
		toReturn[8] = new StatsRow("Weakpoint Bonus:", "+" + convertDoubleToPercentage(getWeakpointBonus()), modIcons.weakpointBonus, selectedTier4 == 1);
		
		toReturn[9] = new StatsRow("Armor Breaking:", convertDoubleToPercentage(getArmorBreaking()), modIcons.armorBreaking, selectedTier4 == 2);
		
		toReturn[10] = new StatsRow("Stun Duration:", getStunDuration(), modIcons.stun, selectedTier5 == 0, selectedTier5 == 0);
		
		toReturn[11] = new StatsRow("Max Penetrations:", getMaxPenetrations(), modIcons.blowthrough, selectedTier4 == 0, selectedTier4 == 0);
		
		boolean recoilModified = selectedTier2 == 1 || selectedOverclock == 3;
		toReturn[12] = new StatsRow("Recoil:", convertDoubleToPercentage(getRecoil()), modIcons.recoil, recoilModified, recoilModified);
		
		toReturn[13] = new StatsRow("Movespeed While Focusing: (m/sec)", getMovespeedWhileFocusing(), modIcons.movespeed, selectedOverclock == 2 || selectedOverclock == 5);
		
		return toReturn;
	}
	
	/****************************************************************************************
	* Other Methods
	****************************************************************************************/

	// Single-target calculations
	@Override
	public double calculateSingleTargetDPS(boolean burst, boolean weakpoint, boolean accuracy, boolean armorWasting) {
		double duration;
		if (burst) {
			duration = ((double) getMagazineSize()) / getRateOfFire();
		}
		else {
			duration = (((double) getMagazineSize()) / getRateOfFire()) + getReloadTime();
		}
		
		double directDamage = getDirectDamage() * getFocusedShotMultiplier();
		
		// Frozen
		if (statusEffects[1]) {
			directDamage *= UtilityInformation.Frozen_Damage_Multiplier;
		}
		// IFG Grenade
		if (statusEffects[3]) {
			directDamage *= UtilityInformation.IFG_Damage_Multiplier;
		}
		
		if (weakpoint && !statusEffects[1]) {
			directDamage = increaseBulletDamageForWeakpoints(directDamage, getWeakpointBonus());
		}
		
		double electroDPS = 0;
		if (selectedOverclock == 4) {
			// Because they get Electrocuted immediately, it has 100% uptime.
			electroDPS = DoTInformation.Electro_DPS;
		}
		
		return (directDamage * getMagazineSize()) / duration + electroDPS;
	}

	@Override
	public double calculateMaxMultiTargetDamage() {
		double totalDamageDealt = calculateMaxNumTargets() * (getMagazineSize() + getCarriedAmmo()) * getDirectDamage() * getFocusedShotMultiplier();
		
		double electrocuteDoTTotalDamage = 0;
		if (selectedOverclock == 4) {
			// OC "Electrocuting Focus Shots" has an increased duration of 4 seconds
			double electrocuteDoTDamagePerEnemy = calculateAverageDoTDamagePerEnemy(0, 4, DoTInformation.Electro_DPS);
			
			double estimatedNumEnemiesKilled = calculateMaxNumTargets() * (calculateFiringDuration() / averageTimeToKill());
			
			electrocuteDoTTotalDamage = electrocuteDoTDamagePerEnemy * estimatedNumEnemiesKilled;
		}
		
		return totalDamageDealt + electrocuteDoTTotalDamage;
	}
	
	@Override
	protected double averageDamageToKillEnemy() {
		double dmgPerShot = increaseBulletDamageForWeakpoints(getDirectDamage() * getFocusedShotMultiplier(), getWeakpointBonus());
		return Math.ceil(EnemyInformation.averageHealthPool() / dmgPerShot) * dmgPerShot;
	}

	@Override
	public double estimatedAccuracy(boolean weakpointAccuracy) {
		// Manually aimed
		return -1.0;
	}
	
	@Override
	public int breakpoints() {
		double[] directDamage = {
			getDirectDamage() * getFocusedShotMultiplier(),  // Kinetic
			0,  // Explosive
			0,  // Fire
			0,  // Frost
			0  // Electric
		};
		
		double[] areaDamage = {
			0,  // Explosive
			0,  // Fire
			0,  // Frost
			0  // Electric
		};
		
		double electroDmg = 0;
		if (selectedOverclock == 4) {
			// OC "Electrocuting Focus Shots" has an increased duration of 4 seconds
			electroDmg = calculateAverageDoTDamagePerEnemy(0, 4, DoTInformation.Electro_DPS);
		}
		double[] DoTDamage = {
			0,  // Fire
			electroDmg,  // Electric
			0,  // Poison
			0  // Radiation
		};
		
		breakpoints = EnemyInformation.calculateBreakpoints(directDamage, areaDamage, DoTDamage, getWeakpointBonus(), 0.0, 0.0, statusEffects[1], statusEffects[3]);
		return MathUtils.sum(breakpoints);
	}

	@Override
	public double utilityScore() {
		// OC "Active Stability System" removes the movespeed penalty while Focusing
		utilityScores[0] = (getMovespeedWhileFocusing() - MathUtils.round(movespeedWhileFocusing * DwarfInformation.walkSpeed, 2)) * UtilityInformation.Movespeed_Utility;
		
		// OC "Hoverclock" gives a 2 second cap to Scout's vertical movement speed (guess: 0.5 m/sec?), but after that 2sec ends original velocity is restored
		if (selectedOverclock == 0) {
			// Because the vertical movespeed cap of +- 0.5 m/sec can be used to negate fall damage from infinite height, there's not really a 
			// way to give this OC a numerical value. For now, I'm just gonna call it 10 and move on.
			utilityScores[0] += 10;
		}
		
		// Light Armor Breaking probability
		utilityScores[2] = calculateProbabilityToBreakLightArmor(getDirectDamage() * getFocusedShotMultiplier(), getArmorBreaking()) * UtilityInformation.ArmorBreak_Utility;
		
		// OC "Electrocuting Focus Shots" = 100% chance to electrocute on focused shots
		if (selectedOverclock == 4) {
			// OC "Electrocuting Focus Shots" has an increased duration of 4 seconds
			utilityScores[3] = calculateMaxNumTargets() * 4 * UtilityInformation.Electrocute_Slow_Utility;
		}
		else {
			utilityScores[3] = 0;
		}
		
		// According to MikeGSG, Mod Tier 5 "Precision Terror" does 1 Fear in a 3.5m radius
		if (selectedTier5 == 1) {
			double probabilityToHitWeakpoint = EnemyInformation.probabilityBulletWillHitWeakpoint();
			int numGlyphidsFeared = calculateNumGlyphidsInRadius(3.5);
			double probabilityToFear = calculateFearProcProbability(1.0);
			// Although it is technically possible to electrocute a Feared enemy with Electrocuting Focus Shots and Blowthrough Rounds, it's so unlikely to happen that I'm choosing not to model that overlap.
			utilityScores[4] = probabilityToHitWeakpoint * probabilityToFear * numGlyphidsFeared * EnemyInformation.averageFearDuration() * UtilityInformation.Fear_Utility;
		}
		else {
			utilityScores[4] = 0;
		}
		
		// Mod Tier 5 "Hitting Where it Hurts" = 100% chance for 3 sec stun
		if (selectedTier5 == 0) {
			utilityScores[5] = calculateMaxNumTargets() * getStunDuration() * UtilityInformation.Stun_Utility;
		}
		else {
			utilityScores[5] = 0;
		}
		
		return MathUtils.sum(utilityScores);
	}
	
	@Override
	public double damagePerMagazine() {
		double bulletDamage = getDirectDamage() * getFocusedShotMultiplier() * getMagazineSize();
		
		double electrocuteDamage = 0;
		if (selectedOverclock == 4) {
			// OC "Electrocuting Focus Shots" has 100% chance to proc and an increased duration of 4 seconds
			electrocuteDamage = calculateAverageDoTDamagePerEnemy(0, 4, DoTInformation.Electro_DPS);
		}
		
		return (bulletDamage + electrocuteDamage) * calculateMaxNumTargets();
	}
	
	@Override
	public double damageWastedByArmor() {
		double weakpointAccuracy = EnemyInformation.probabilityBulletWillHitWeakpoint() * 100.0;
		damageWastedByArmorPerCreature = EnemyInformation.percentageDamageWastedByArmor(getDirectDamage() * getFocusedShotMultiplier(), 0.0, getArmorBreaking(), getWeakpointBonus(), 100.0, weakpointAccuracy);
		return 100 * MathUtils.vectorDotProduct(damageWastedByArmorPerCreature[0], damageWastedByArmorPerCreature[1]) / MathUtils.sum(damageWastedByArmorPerCreature[0]);
	}
}
