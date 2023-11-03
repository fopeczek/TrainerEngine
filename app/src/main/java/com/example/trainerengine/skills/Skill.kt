package com.example.trainerengine.skills

import com.example.trainerengine.database.Database

class SkillSet(private val skillSetID: Int, private val moduleID: Int, private val database: Database, private val skills: MutableList<Skill> = mutableListOf()) {
    fun getSkillSetID(): Int {
        return skillSetID
    }

    fun getModuleID(): Int {
        return moduleID
    }

    fun getSkills(): MutableList<Skill> {
        return skills
    }

    fun getSkill(name: String): Skill?{
        for (skill in skills) {
            if (skill.getName() == name) {
                return skill
            }
        }
        return null
    }

    fun addSkill(skill: Skill) {
        skills.add(skill)
    }

    fun contains(skill: Skill): Boolean {
        return skills.contains(skill)
    }

    fun contains(subSet: SkillSet): Boolean {
        return skills.containsAll(subSet.skills)
    }
}

class Skill(private val skillID: Int, private val skillSetID: Int, private val name: String, private val description: String) {
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
}