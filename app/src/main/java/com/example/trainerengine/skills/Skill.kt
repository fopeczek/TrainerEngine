package com.example.trainerengine.skills

import com.example.trainerengine.database.Database

class SkillSet(
    private val skillSetID: Int,
    private val moduleID: Int,
    private val sessionID: Int,
    private val database: Database,
    private val skills: MutableList<Skill> = mutableListOf()
) {
    fun getSkillSetID(): Int {
        return skillSetID
    }

    fun getModuleID(): Int {
        return moduleID
    }

    fun getSessionID(): Int {
        return sessionID
    }

    fun getSkills(): MutableList<Skill> {
        return skills
    }

    fun getSkill(name: String): Skill? {
        for (skill in skills) {
            if (skill.getName() == name) {
                return skill
            }
        }
        return null
    }

    fun addSkill(skill: Skill) {
        skills.add(skill)
        database.updateSkillSet(this)
    }

    fun contains(skill: Skill): Boolean {
        return skills.contains(skill)
    }

    fun contains(subSet: SkillSet): Boolean {
        return skills.containsAll(subSet.skills)
    }
}

class Skill(
    private val skillID: Int,
    private val skillSetID: Int,
    private val name: String,
    private val description: String,
    private var score: Float,
    private var visibility: Boolean,
    private val database: Database
) {
    fun getSkillID(): Int {
        return skillID
    }

    fun getSkillSetID(): Int {
        return skillSetID
    }

    fun getName(): String {
        return name
    }

    fun getDescription(): String {
        return description
    }

    fun getScore(): Float {
        return score
    }

    fun setScore(score: Float) {
        this.score = score
        database.updateSkill(this)
    }

    fun getVisibility(): Boolean {
        return visibility
    }

    fun setVisibility(visibility: Boolean) {
        this.visibility = visibility
        database.updateSkill(this)
    }
}