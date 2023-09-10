from typing import Optional

import java

ModuleConfig = java.jclass("com.example.trainerengine.configs.ModuleConfig")
ConfigData = java.jclass("com.example.trainerengine.configs.ConfigData")


def make_task(config: ModuleConfig) -> Optional[tuple]:
    pass  # return tuple of (question, answer)


def check_answer(user_answer, answer) -> bool:
    pass  # return True if answer is correct
